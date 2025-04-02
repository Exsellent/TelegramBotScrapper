package backend.academy.bot.service;

import backend.academy.bot.insidebot.TelegramBotService;
import backend.academy.bot.dto.LinkUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaMessageConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageConsumer.class);
    private final TelegramBotService telegramBotService;

    @Autowired
    public KafkaMessageConsumer(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @KafkaListener(topics = "${app.kafka.topics.notifications}", groupId = "bot-group")
    public void listen(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            LinkUpdateRequest update = mapper.readValue(message, LinkUpdateRequest.class);
            // Отправляем сообщение в каждый чат из списка
            for (Long chatId : update.getTgChatIds()) {
                telegramBotService.sendChatMessage(chatId, update.getDescription());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse Kafka message: {}", message, e);
        }
    }
}
