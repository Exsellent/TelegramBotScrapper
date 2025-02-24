package backend.academy.scrapper.exception;

public class LinkAlreadyAddedException extends RuntimeException {
    public LinkAlreadyAddedException(String message) {
        super(message);
    }
}
