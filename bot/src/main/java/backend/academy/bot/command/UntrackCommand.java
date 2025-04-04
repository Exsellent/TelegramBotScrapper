package backend.academy.bot.command;

import backend.academy.bot.dto.LinkResponse;
import backend.academy.bot.utils.LinkParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
public class UntrackCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(UntrackCommand.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisTemplate<String, List<LinkResponse>> redisTemplate;
    private final ObjectMapper objectMapper;
    private final String linkCommandsTopic;

    @Autowired
    public UntrackCommand(
        KafkaTemplate<String, String> kafkaTemplate,
        RedisTemplate<String, List<LinkResponse>> redisTemplate,
        ObjectMapper objectMapper,
        @Value("${app.kafka.topics.link-commands}") String linkCommandsTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.linkCommandsTopic = linkCommandsTopic;
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
        String[] parts = update.message().text().split(" ", 2);

        if (parts.length < 2 || !LinkParser.isValidURL(parts[1])) {
            return new SendMessage(chatId, "Enter the correct URL to delete (e.g., /untrack https://example.com).");
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(Map.of("command", "remove", "link", parts[1]));
            kafkaTemplate.send(linkCommandsTopic, chatId.toString(), jsonMessage);
            redisTemplate.delete("tracked-links:" + chatId);
            LOGGER.info("Sent link removal request for chat {}: {}", chatId, parts[1]);
            return new SendMessage(chatId, "Link removal request sent: " + parts[1]);
        } catch (Exception e) {
            LOGGER.error("Error sending link removal request for chat {}", chatId, e);
            return new SendMessage(chatId, "Error sending link removal request.");
        }
    }
}
