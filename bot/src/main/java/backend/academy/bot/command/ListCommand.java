package backend.academy.bot.command;

import backend.academy.bot.dto.LinkResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ListCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListCommand.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisTemplate<String, List<LinkResponse>> redisTemplate;
    private final ObjectMapper objectMapper;
    private final String linkCommandsTopic;

    @Autowired
    public ListCommand(
            KafkaTemplate<String, String> kafkaTemplate,
            RedisTemplate<String, List<LinkResponse>> redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.link-commands}") String linkCommandsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.linkCommandsTopic = linkCommandsTopic;
    }

    @Override
    public String command() {
        return "/list";
    }

    @Override
    public String description() {
        return "Show a list of tracked links";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        LOGGER.info("Handling /list command for chat {}", chatId);
        String cacheKey = "tracked-links:" + chatId;

        try {
            // Проверяем кэш
            List<LinkResponse> cachedLinks = redisTemplate.opsForValue().get(cacheKey);
            if (cachedLinks != null && !cachedLinks.isEmpty()) {
                LOGGER.info("Returning cached links for chat {}", chatId);
                return buildResponse(chatId, cachedLinks);
            }

            // Отправляем запрос в Kafka
            String json = objectMapper.writeValueAsString(Map.of("command", "list"));
            kafkaTemplate.send(linkCommandsTopic, String.valueOf(chatId), json);

            Thread.sleep(1000);
            List<LinkResponse> links = redisTemplate.opsForValue().get(cacheKey);
            return buildResponse(chatId, links != null ? links : Collections.emptyList());
        } catch (Exception e) {
            LOGGER.error("Error handling /list command", e);
            return new SendMessage(chatId, "An error occurred while fetching links.");
        }
    }

    private SendMessage buildResponse(Long chatId, List<LinkResponse> links) {
        if (links.isEmpty()) {
            return new SendMessage(chatId, "The list of tracked links is empty.");
        }
        StringBuilder messageBuilder = new StringBuilder("Tracked links:\n");
        for (LinkResponse link : links) {
            messageBuilder.append(link.getUrl()).append("\n");
        }
        redisTemplate.opsForValue().set("tracked-links:" + chatId, links, 10, TimeUnit.MINUTES);
        return new SendMessage(chatId, messageBuilder.toString());
    }
}
