package backend.academy.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelpCommand.class);
    private final List<Command> commands;

    @Autowired
    public HelpCommand(List<Command> commands) {
        // Исключаем сам HelpCommand, чтобы избежать рекурсии
        this.commands =
            commands.stream().filter(cmd -> !(cmd instanceof HelpCommand)).collect(Collectors.toList());
    }

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public String description() {
        return "List of available commands";
    }

    @Override
    public SendMessage handle(Update update) {
        StringBuilder messageText = new StringBuilder("Available Commands:\n");

        for (Command command : commands) {
            messageText
                .append(command.command())
                .append(": ")
                .append(command.description())
                .append("\n");
        }

        messageText
            .append("\nSupported link formats:\n")
            .append("GitHub: https://github.com/example/repo\n")
            .append("StackOverflow: https://stackoverflow.com/questions/example");

        LOGGER.info("Handling /help command");

        return new SendMessage(update.message().chat().id(), messageText.toString());
    }
}
