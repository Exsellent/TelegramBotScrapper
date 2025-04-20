package backend.academy.scrapper.filter;

import backend.academy.scrapper.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@AllArgsConstructor
public class RateLimitingFilter implements Filter {

    private final ObjectMapper objectMapper;
    private static final int REQUEST_LIMIT = 100;
    static final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String clientIp = request.getRemoteAddr();
        requestCounts.putIfAbsent(clientIp, new AtomicInteger(0));
        int requestCount = requestCounts.get(clientIp).incrementAndGet();

        if (requestCount > REQUEST_LIMIT) {
            ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                    .description("Too many requests")
                    .code(HttpStatus.TOO_MANY_REQUESTS.toString())
                    .exceptionName("RateLimitExceeded")
                    .exceptionMessage("You have exceeded the request limit")
                    .stacktrace(null)
                    .build();

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; sandbox");
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        } else {
            chain.doFilter(servletRequest, servletResponse);
        }
    }

    public static void clearRequestCounts() {
        requestCounts.clear();
    }
}
