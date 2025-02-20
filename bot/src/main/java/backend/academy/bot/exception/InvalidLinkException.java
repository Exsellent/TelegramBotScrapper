package backend.academy.bot.exception;

public class InvalidLinkException extends ApiException {

    public InvalidLinkException(String message) {
        super(message);
    }

    @Override
    public int getStatusCode() {
        return 400; // Возвращаем 400 для неверных ссылок (Bad Request)
    }
}
