package backend.academy.bot.command;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.response.BaseResponse;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BotMenuInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotMenuInitializer.class);
    private final List<Command> commands;
    private final TelegramBot bot;

    @Autowired
    public BotMenuInitializer(List<Command> commands, TelegramBot bot) {
        this.commands = commands;
        this.bot = bot;
    }

    @PostConstruct
    public void initializeCommands() {
        List<BotCommand> botCommands =
                commands.stream().map(Command::toApiCommand).collect(Collectors.toList());

        SetMyCommands setMyCommands = new SetMyCommands(botCommands.toArray(new BotCommand[0]));

        try {
            BaseResponse response = bot.execute(setMyCommands);
            if (response.isOk()) {
                LOGGER.info("Bot commands menu successfully initialized with {} commands", botCommands.size());
            } else {
                LOGGER.error("Failed to initialize bot commands menu: {}", response.description());
            }
        } catch (Exception e) {
            LOGGER.error("Error initializing bot commands menu", e);
        }
    }
}
