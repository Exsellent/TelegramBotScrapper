package backend.academy.bot.exception;

public class FilterValidationException extends ApiException {

    public FilterValidationException(String message) {
        super(message);
    }

    @Override
    public int getStatusCode() {
        return 422; // Возвращаем 422 Unprocessable Entity для некорректных фильтров
    }
}
