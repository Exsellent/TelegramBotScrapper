package backend.academy.scrapper.exception;

import backend.academy.scrapper.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = {IllegalArgumentException.class})
    protected ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .description("Bad request")
                .code(HttpStatus.BAD_REQUEST.toString())
                .exceptionName(ex.getClass().getSimpleName())
                .exceptionMessage(ex.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {Exception.class})
    protected ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .description("Internal server error")
                .code(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                .exceptionName(ex.getClass().getSimpleName())
                .exceptionMessage(ex.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ChatAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleChatAlreadyRegisteredException(ChatAlreadyRegisteredException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Chat already registered",
                HttpStatus.BAD_REQUEST.toString(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LinkAlreadyAddedException.class)
    public ResponseEntity<ApiErrorResponse> handleLinkAlreadyAddedException(LinkAlreadyAddedException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Link already added",
                HttpStatus.BAD_REQUEST.toString(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleChatNotFoundException(ChatNotFoundException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Chat not found",
                HttpStatus.NOT_FOUND.toString(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                null);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(LinkNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleChatNotFoundException(LinkNotFoundException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Link not found",
                HttpStatus.NOT_FOUND.toString(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                null);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}
