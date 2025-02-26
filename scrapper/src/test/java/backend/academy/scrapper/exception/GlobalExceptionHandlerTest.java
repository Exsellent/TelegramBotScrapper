package backend.academy.scrapper.exception;

import backend.academy.scrapper.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    public void whenIllegalArgumentExceptionThrown_thenReturnsBadRequest() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input");
        WebRequest request = mock(WebRequest.class);

        // Act
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad request", response.getBody().getDescription());
        assertEquals("IllegalArgumentException", response.getBody().getExceptionName());
        assertEquals("Invalid input", response.getBody().getExceptionMessage());
    }

    @Test
    public void whenChatNotFoundExceptionThrown_thenReturnsNotFound() {
        // Arrange
        ChatNotFoundException ex = new ChatNotFoundException("Chat with ID 123 not found");
        WebRequest request = mock(WebRequest.class);

        // Act
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleChatNotFoundException(ex);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Chat not found", response.getBody().getDescription());
        assertEquals("ChatNotFoundException", response.getBody().getExceptionName());
        assertEquals("Chat with ID 123 not found", response.getBody().getExceptionMessage());
    }
}
