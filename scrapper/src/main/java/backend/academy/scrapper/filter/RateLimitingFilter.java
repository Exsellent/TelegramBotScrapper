package backend.academy.scrapper.filter;

import backend.academy.scrapper.configuration.RateLimitingConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
public class RateLimitingFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final RateLimitingConfig config;
    private final Map<String, RequestCount> requestCounts = new ConcurrentHashMap<>();

    public RateLimitingFilter(ObjectMapper objectMapper, RateLimitingConfig config) {
        this.objectMapper = objectMapper;
        this.config = config;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientId = request.getHeader("X-Client-Id");
        if (clientId == null || clientId.isEmpty()) {
            clientId = request.getRemoteAddr();
        }

        System.out.println("RateLimitingFilter: Processing request for " + request.getRequestURI());
        System.out.println("Client ID: " + clientId);

        // Защита от изменений clientId в процессе обработки
        final String finalClientId = clientId;

        // Получаем или создаем счетчик для клиента
        RequestCount requestCount = requestCounts.computeIfAbsent(finalClientId, k -> new RequestCount());

        long currentTime = System.currentTimeMillis();
        int currentCount;

        // Пытаемся получить текущий счетчик с защитой от гонок
        synchronized (requestCount) {
            // Если временное окно истекло, сбрасываем счетчик
            if (currentTime - requestCount.getStartTime() > config.getWindowSeconds() * 1000L) {
                requestCount.reset(currentTime);
            }

            // Увеличиваем счетчик и получаем текущее значение
            currentCount = requestCount.incrementAndGet();

            System.out.println("Request count for " + finalClientId + ": " + currentCount + ", limit: "
                    + config.getRequestLimit());
        }

        // Проверяем, не превышен ли лимит запросов
        if (currentCount > config.getRequestLimit()) {
            System.out.println("Rate limit exceeded for client: " + finalClientId);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(config.getWindowSeconds()));
            response.setContentType("application/json");
            String errorResponse = objectMapper.writeValueAsString(Map.of(
                    "code", "429",
                    "exceptionName", "RateLimitExceeded",
                    "description", "Too many requests"));
            response.getWriter().write(errorResponse);
            // Завершаем обработку запроса
            return;
        }

        // Продолжаем обработку запроса
        filterChain.doFilter(request, response);
    }

    // Метод для тестов - очищает счетчики запросов
    public void clearRequestCountsForTest() {
        requestCounts.clear();
        System.out.println("Request counts cleared for test");
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
