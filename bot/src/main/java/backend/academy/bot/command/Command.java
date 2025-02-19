package backend.academy.bot.command;

import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public interface Command {
    String command();

    String description();

    SendMessage handle(Update update);

    default boolean supports(Update update) {
        try {
            Pattern pattern = Pattern.compile("\\s+");
            StringTokenizer tokenizer = new StringTokenizer(update.message().text(), pattern.pattern());
            return tokenizer.hasMoreTokens() && Objects.equals(command(), tokenizer.nextToken());
        } catch (Exception e) {
            return false;
        }
    }

    default BotCommand toApiCommand() {
        return new BotCommand(command(), description());
    }
}
