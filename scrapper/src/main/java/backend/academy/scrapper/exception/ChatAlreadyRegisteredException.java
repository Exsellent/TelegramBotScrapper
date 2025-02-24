package backend.academy.scrapper.exception;

public class ChatAlreadyRegisteredException extends RuntimeException {
    public ChatAlreadyRegisteredException(String message) {
        super(message);
    }
}
