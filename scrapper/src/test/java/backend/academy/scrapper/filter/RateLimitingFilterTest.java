package backend.academy.scrapper.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class RateLimitingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private RateLimitingFilter filter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        filter = new RateLimitingFilter(objectMapper);
        RateLimitingFilter.requestCounts.clear(); // Очистка счетчика перед каждым тестом
    }

    @Test
    public void doFilter_allowsRequest_whenUnderLimit() throws Exception {
        // Arrange
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        for (int i = 0; i < 50; i++) { // Меньше лимита
            filter.doFilter(request, response, chain);
        }

        // Assert
        verify(chain, times(50)).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    public void doFilter_blocksRequest_whenLimitExceeded() throws Exception {
        // Arrange
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);

        // Act
        for (int i = 0; i < 101; i++) { // Превышаем лимит
            filter.doFilter(request, response, chain);
        }

        // Assert
        verify(chain, times(100)).doFilter(request, response);
        verify(response, times(1)).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response, times(1)).setContentType("application/json");

        String result = sw.toString();
        ApiErrorResponse responseObj = objectMapper.readValue(result, ApiErrorResponse.class);

        assertTrue(result.contains("Too many requests"));
        assertEquals("Too many requests", responseObj.getDescription());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.toString(), responseObj.getCode());
    }
}
