package backend.academy.scrapper.filter;

import backend.academy.scrapper.configuration.RateLimitingConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitingFilter.class);
    private final ObjectMapper objectMapper;
    private final RateLimitingConfig config;
    private final MeterRegistry meterRegistry;
    private final Map<String, RequestCount> requestCounts = new ConcurrentHashMap<>();

    public RateLimitingFilter(ObjectMapper objectMapper, RateLimitingConfig config, MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.config = config;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientId = request.getHeader("X-Client-Id");
        if (clientId == null || clientId.isEmpty()) {
            clientId = request.getRemoteAddr();
        }

        LOGGER.debug("Processing request for {}", request.getRequestURI());
        LOGGER.debug("Client ID: {}", clientId);

        // Увеличиваем счетчик запросов
        meterRegistry
                .counter("scrapper.api.requests", "endpoint", request.getRequestURI())
                .increment();

        // Защита от изменений clientId в процессе обработки
        final String finalClientId = clientId;

        // счетчик для клиента
        RequestCount requestCount = requestCounts.computeIfAbsent(finalClientId, k -> new RequestCount());

        long currentTime = System.currentTimeMillis();
        int currentCount;

        //  текущий счетчик с защитой от гонок
        synchronized (requestCount) {
            // Если временное окно истекло, сброс счетчика
            if (currentTime - requestCount.getStartTime() > config.getWindowSeconds() * 1000L) {
                LOGGER.debug("Resetting request count for client {} as window has expired", finalClientId);
                requestCount.reset(currentTime);
            }

            // Увеличиваем счетчик и получаем текущее значение
            currentCount = requestCount.incrementAndGet();

            LOGGER.debug("Request count for {}: {}, limit: {}", finalClientId, currentCount, config.getRequestLimit());
        }

        // Проверяем, не превышен ли лимит запросов
        if (currentCount > config.getRequestLimit()) {
            LOGGER.warn("Rate limit exceeded for client: {}", finalClientId);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // Исправлено с 429

            response.setStatus(429);

            response.setHeader("Retry-After", String.valueOf(config.getWindowSeconds()));
            response.setContentType("application/json");
            String errorResponse = objectMapper.writeValueAsString(Map.of(
                    "code", String.valueOf(HttpStatus.TOO_MANY_REQUESTS.value()),
                    "exceptionName", "RateLimitExceeded",
                    "description", "Too many requests"));
            response.getWriter().write(errorResponse);
            return;
        }

        // Продолжаем обработку запроса
        filterChain.doFilter(request, response);
    }

    // Метод для тестов - очищает счетчики запросов
    public void clearRequestCountsForTest() {
        requestCounts.clear();
        LOGGER.debug("Request counts cleared for test");
    }

    // Класс для хранения информации о количестве запросов
    private static class RequestCount {
        private long startTime;
        private final AtomicInteger count;

        RequestCount() {
            this.startTime = System.currentTimeMillis();
            this.count = new AtomicInteger(0);
        }

        void reset(long newStartTime) {
            this.startTime = newStartTime;
            this.count.set(0);
        }

        int incrementAndGet() {
            return this.count.incrementAndGet();
        }

        long getStartTime() {
            return startTime;
        }

        int getCount() {
            return count.get();
        }
    }
}
