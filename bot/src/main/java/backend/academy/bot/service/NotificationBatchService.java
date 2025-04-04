package backend.academy.bot.service;

import backend.academy.bot.dto.LinkUpdateRequest;
import backend.academy.bot.insidebot.TelegramBotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationBatchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationBatchService.class);


        private final TelegramBotService telegramBotService;
        private final RedisTemplate<String, String> redisTemplate;
        private final ObjectMapper objectMapper;

        @Autowired
        public NotificationBatchService(TelegramBotService telegramBotService, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
            this.telegramBotService = telegramBotService;
            this.redisTemplate = redisTemplate;
            this.objectMapper = objectMapper;
        }

        @Scheduled(cron = "${app.batch.notification-cron:0 0 10 * * ?}") // Каждое утро в 10:00
        public void sendDailyDigest() {
            Set<String> keys = redisTemplate.keys("notifications:*");
            for (String key : keys) {
                Long chatId = Long.parseLong(key.split(":")[1]);
                List<String> notifications = redisTemplate.opsForList().range(key, 0, -1);
                if (notifications != null && !notifications.isEmpty()) {
                    StringBuilder digest = new StringBuilder("Daily Digest:\n");
                    for (String message : notifications) {
                        try {
                            LinkUpdateRequest update = objectMapper.readValue(message, LinkUpdateRequest.class);
                            digest.append(update.getDescription()).append("\n\n");
                        } catch (Exception e) {
                            LOGGER.error("Failed to parse notification: {}", message, e);
                        }
                    }
                    telegramBotService.sendChatMessage(chatId, digest.toString());
                    redisTemplate.delete(key); // Очищаем после отправки
                }
            }
        }
    }
