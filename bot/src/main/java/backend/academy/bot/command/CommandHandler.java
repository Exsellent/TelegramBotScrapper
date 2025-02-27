package backend.academy.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CommandHandler {

    private final List<Command> commands;

    public CommandHandler(List<Command> commands) {
        this.commands = commands;
    }

    public SendMessage handleUpdate(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return null;
        }

        for (Command command : commands) {
            if (command.supports(update)) {
                return command.handle(update);
            }
        }

        // Обработка неизвестной команды
        if (update.message().text().startsWith("/")) {
            return new SendMessage(
                    update.message().chat().id(), "Unknown command. Use /help to view the list of available commands.");
        }

        return null;
    }
}
