package backend.academy.bot.exception;

public class ApiException extends RuntimeException implements ApiError {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorMessage() {
        return super.getMessage();
    }

    @Override
    public int getStatusCode() {
        return 500; // По умолчанию можно вернуть внутреннюю ошибку сервера
    }
}
