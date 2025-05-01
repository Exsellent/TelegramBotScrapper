package backend.academy.bot.command;

import backend.academy.bot.service.KafkaService;
import backend.academy.bot.service.RedisCacheService;
import backend.academy.bot.utils.LinkParser;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UntrackCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(UntrackCommand.class);
    private static final int EXPECTED_PARTS_COUNT = 2;
    private static final int URL_INDEX = 1;

    private final KafkaService kafkaService;
    private final RedisCacheService redisCacheService;

    @Autowired
    public UntrackCommand(KafkaService kafkaService, RedisCacheService redisCacheService) {
        this.kafkaService = kafkaService;
        this.redisCacheService = redisCacheService;
    }

    @Override
    public String command() {
        return "/untrack";
    }

    @Override
    public String description() {
        return "Stop tracking a specific link";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        String[] parts = update.message().text().split(" ", EXPECTED_PARTS_COUNT);

        if (parts.length < EXPECTED_PARTS_COUNT || !LinkParser.isValidURL(parts[URL_INDEX])) {
            return new SendMessage(chatId, "Enter the correct URL to delete (e.g., /untrack https://example.com).");
        }

        String url = parts[URL_INDEX];
        try {
            kafkaService.sendCommand(chatId, "remove", Map.of("link", url));
            redisCacheService.removeLink(chatId, url);
            LOGGER.info("Sent link removal request for chat {}: {}", chatId, url);
            return new SendMessage(chatId, "Link removal request sent: " + url);
        } catch (Exception e) {
            LOGGER.error("Error sending link removal request for chat {}", chatId, e);
            return new SendMessage(chatId, "Error sending link removal request.");
        }
    }
}
