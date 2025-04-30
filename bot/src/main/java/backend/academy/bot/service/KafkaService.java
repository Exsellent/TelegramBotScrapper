package backend.academy.bot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaService {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String linkCommandsTopic;

    @Autowired
    public KafkaService(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.link-commands}") String linkCommandsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.linkCommandsTopic = linkCommandsTopic;
    }

    public void sendCommand(Long chatId, String command, Map<String, Object> data) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("command", command);
            message.putAll(data);
            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(linkCommandsTopic, chatId.toString(), json);
            LOGGER.info("Sent Kafka command {} for chat {}", command, chatId);
        } catch (Exception e) {
            LOGGER.error("Error sending command to Kafka for chat {}: {}", chatId, e.getMessage());
            throw new RuntimeException("Failed to send Kafka message", e);
        }
    }
}
