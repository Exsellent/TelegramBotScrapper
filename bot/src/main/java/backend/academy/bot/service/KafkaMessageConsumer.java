package backend.academy.bot.service;

import backend.academy.bot.dto.LinkUpdateRequest;
import backend.academy.bot.insidebot.TelegramBotService;
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
    private final RedisTemplate<String, String> redisTemplate; // Для хранения уведомлений

    @Autowired
    public KafkaMessageConsumer(TelegramBotService telegramBotService, RedisTemplate<String, String> redisTemplate) {
        this.telegramBotService = telegramBotService;
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "${app.kafka.topics.notifications}", groupId = "bot-group")
    public void listen(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            LinkUpdateRequest update = mapper.readValue(message, LinkUpdateRequest.class);
            for (Long chatId : update.getTgChatIds()) {
                String key = "notifications:" + chatId;
                redisTemplate.opsForList().rightPush(key, message); // Сохраняем уведомление в список
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse Kafka message: {}", message, e);
        }
    }
}
