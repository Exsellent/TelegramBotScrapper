package backend.academy.bot.exception;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException(String message) {
        super(message);
    }

    public static String getExceptionName() {
        return "LinkNotFoundException";
    }
}
