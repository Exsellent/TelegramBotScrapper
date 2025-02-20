package backend.academy.bot.exception;

public interface ApiError {
    String getErrorMessage();

    int getStatusCode();
}
