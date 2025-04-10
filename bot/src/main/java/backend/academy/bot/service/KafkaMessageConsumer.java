package backend.academy.bot.service;

import backend.academy.bot.insidebot.TelegramBotService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaMessageConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageConsumer.class);
    private final TelegramBotService telegramBotService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaMessageConsumer(
        TelegramBotService telegramBotService,
        RedisTemplate<String, String> redisTemplate,
        ObjectMapper objectMapper
    ) {
        this.telegramBotService = telegramBotService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.notifications}", groupId = "bot-group")
    public void listen(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            JsonNode chatIdsNode = jsonNode.get("tgChatIds");
            String description = jsonNode.get("description").asText();

            if (chatIdsNode != null && chatIdsNode.isArray()) {
                for (JsonNode chatIdNode : chatIdsNode) {
                    Long chatId = chatIdNode.asLong();
                    String mode = redisTemplate.opsForValue().get("notification-mode:" + chatId);
                    if ("instant".equals(mode) || mode == null) { // По умолчанию — мгновенные
                        telegramBotService.sendChatMessage(chatId, description);
                    } else { // Режим digest
                        String key = "notifications:" + chatId;
                        redisTemplate.opsForList().rightPush(key, message);
                        LOGGER.info("Stored notification for chat {} in Redis", chatId);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse Kafka message: {}", message, e);
        }
    }
}
