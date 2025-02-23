package backend.academy.bot.service;

import backend.academy.bot.command.Command;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserMessageProcessorImpl implements UserMessageProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserMessageProcessorImpl.class);
    private final List<Command> commands;

    @Autowired
    public UserMessageProcessorImpl(List<Command> commands) {
        this.commands = commands;
        if (this.commands.isEmpty()) {
            LOGGER.warn("No commands were initialized in UserMessageProcessor");
        } else {
            LOGGER.info("Initialized UserMessageProcessor with {} commands", this.commands.size());
        }
    }

    @Override
    public List<Command> getCommands() {
        return commands;
    }

    @Override
    public SendMessage process(Update update) {
        if (update.message() == null || update.message().text() == null) {
            LOGGER.warn("Received update with null message or text");
            return null;
        }

        String messageText = update.message().text().trim();
        Long chatId = update.message().chat().id();

        try {
            for (Command command : commands) {
                if (messageText.startsWith(command.command())) {
                    LOGGER.debug("Processing command: {} for chat: {}", command.command(), chatId);
                    return command.handle(update);
                }
            }

            LOGGER.debug("Unknown command received: {} from chat: {}", messageText, chatId);
            return new SendMessage(chatId, "Unknown team. " + "Use /help for the list" + "available commands.");

        } catch (Exception e) {
            LOGGER.error("Error processing message: {} from chat: {}", messageText, chatId, e);
            return new SendMessage(
                    chatId, "An error occurred while processing the command." + "Please try again later..");
        }
    }
}
