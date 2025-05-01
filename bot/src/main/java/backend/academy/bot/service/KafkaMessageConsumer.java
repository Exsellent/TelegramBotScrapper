package backend.academy.bot.service;

import backend.academy.bot.command.ListCommand;
import backend.academy.bot.dto.LinkResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaMessageConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageConsumer.class);
    private static final String TRACKED_LINKS_PREFIX = "tracked-links:";

    private final RedisCacheService redisCacheService;
    private final Map<Long, CompletableFuture<List<LinkResponse>>> pendingRequests;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaMessageConsumer(
            RedisCacheService redisCacheService, ListCommand listCommand, ObjectMapper objectMapper) {
        this.redisCacheService = redisCacheService;
        this.pendingRequests = listCommand.getPendingRequests();
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.link-commands}")
    public void listen(ConsumerRecord<String, String> record) {
        Long chatId = Long.valueOf(record.key());
        String message = record.value();
        LOGGER.info("Received Kafka message for chat {}: {}", chatId, message);

        // Обработка сообщения
        List<LinkResponse> links = processMessage(message);
        redisCacheService.setLinks(chatId, links);

        // Завершаем CompletableFuture и удаляем из pendingRequests
        CompletableFuture<List<LinkResponse>> future = pendingRequests.remove(chatId);
        if (future != null) {
            future.complete(links);
            LOGGER.info("Completed future for chat {}", chatId);
        } else {
            LOGGER.warn("No pending future found for chat {}", chatId);
        }
    }

    private List<LinkResponse> processMessage(String message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, Map.class);
            String command = (String) data.get("command");

            switch (command) {
                case "list":
                    return new ArrayList<>();
                case "add":
                case "remove":
                    LOGGER.info("Command {} not fully implemented in KafkaMessageConsumer", command);
                    return new ArrayList<>();
                default:
                    LOGGER.warn("Unknown command: {}", command);
                    return new ArrayList<>();
            }
        } catch (Exception e) {
            LOGGER.error("Error processing Kafka message: {}", message, e);
            return new ArrayList<>();
        }
    }
}
