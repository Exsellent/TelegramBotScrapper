package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.LinkUpdateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.message-transport", havingValue = "Kafka")
public class KafkaNotificationService implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaNotificationService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String notificationTopic;
    private final String dlqTopic;

    @Autowired
    public KafkaNotificationService(
        KafkaTemplate<String, String> kafkaTemplate,
        ObjectMapper objectMapper,
        @Value("${app.kafka.topics.notifications}") String notificationTopic,
        @Value("${app.kafka.topics.dlq}") String dlqTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.notificationTopic = notificationTopic;
        this.dlqTopic = dlqTopic;

        LOGGER.info("KafkaNotificationService initialized with notificationTopic: {}, dlqTopic: {}", notificationTopic, dlqTopic);
    }

    @Override
    public void sendNotification(LinkUpdateRequest update) {
        try {
            String json = objectMapper.writeValueAsString(update);
            kafkaTemplate.send(notificationTopic, json);
            LOGGER.info("Sent message to Kafka topic {}: {}", notificationTopic, json);
        } catch (JsonProcessingException e) {
            kafkaTemplate.send(dlqTopic, update.toString());
            LOGGER.error("Failed to serialize LinkUpdateRequest. Sent to DLQ topic {}: {}", dlqTopic, update, e);
        }
    }
}
