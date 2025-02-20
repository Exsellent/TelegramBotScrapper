package backend.academy.bot.exception;

public class ChatAlreadyRegisteredException extends RuntimeException {
    public ChatAlreadyRegisteredException(String message) {
        super(message);
    }

    public static String getExceptionName() {
        return "ChatAlreadyRegisteredException";
    }
}
