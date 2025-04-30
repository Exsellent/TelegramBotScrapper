package backend.academy.bot.service;

import backend.academy.bot.insidebot.TelegramBotService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NotificationBatchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationBatchService.class);
    private final TelegramBotService telegramBotService;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;

    @Autowired
    public NotificationBatchService(
            TelegramBotService telegramBotService, RedisCacheService redisCacheService, ObjectMapper objectMapper) {
        this.telegramBotService = telegramBotService;
        this.redisCacheService = redisCacheService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "${app.batch.notification-cron:0 0 10 * * ?}")
    public void sendDailyDigest() {
        Set<String> keys = redisCacheService.getNotificationKeys();
        if (keys == null || keys.isEmpty()) {
            LOGGER.info("No notifications to send");
            return;
        }

        for (String key : keys) {
            Long chatId = Long.parseLong(key.split(":")[1]);
            List<String> notifications = redisCacheService.getNotifications(chatId);
            if (notifications != null && !notifications.isEmpty()) {
                StringBuilder digest = new StringBuilder("📬 Daily Digest:\n\n");
                boolean hasValidEntries = false;

                for (String message : notifications) {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(message);
                        String description = jsonNode.get("description").asText();
                        digest.append(description).append("\n\n");
                        hasValidEntries = true;
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse notification: {}", message, e);
                    }
                }

                if (hasValidEntries) {
                    telegramBotService.sendChatMessage(chatId, digest.toString());
                    LOGGER.info("Sent digest to chat {}", chatId);
                } else {
                    LOGGER.info("No valid notifications for chat {}", chatId);
                }

                redisCacheService.deleteNotifications(chatId);
            }
        }
    }
}
