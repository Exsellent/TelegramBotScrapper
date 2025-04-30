package backend.academy.bot.service;

import backend.academy.bot.command.ListCommand;
import backend.academy.bot.dto.LinkResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaMessageConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageConsumer.class);
    private final RedisTemplate<String, List<LinkResponse>> redisTemplate;
    private final Map<Long, CompletableFuture<List<LinkResponse>>> pendingRequests;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaMessageConsumer(
            RedisTemplate<String, List<LinkResponse>> redisTemplate,
            ListCommand listCommand,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.pendingRequests = listCommand.getPendingRequests();
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.link-commands}")
    public void listen(ConsumerRecord<String, String> record) {
        Long chatId = Long.valueOf(record.key());
        String message = record.value();
        LOGGER.info("Received Kafka message for chat {}: {}", chatId, message);

        // Обработка сообщения и запись результата в Redis
        List<LinkResponse> links = processMessage(message);
        redisTemplate.opsForValue().set("tracked-links:" + chatId, links, 10, TimeUnit.MINUTES);

        // Завершаем CompletableFuture
        CompletableFuture<List<LinkResponse>> future = pendingRequests.get(chatId);
        if (future != null) {
            future.complete(links);
        }
    }

    private List<LinkResponse> processMessage(String message) {
        try {

            Map<String, Object> data = objectMapper.readValue(message, Map.class);
            String command = (String) data.get("command");

            if ("list".equals(command)) {
                return new ArrayList<>();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            LOGGER.error("Error processing Kafka message: {}", message, e);
            return new ArrayList<>();
        }
    }
}
