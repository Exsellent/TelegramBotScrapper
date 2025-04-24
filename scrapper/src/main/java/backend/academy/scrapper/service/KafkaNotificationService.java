package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.LinkUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@ConditionalOnProperty(name = "app.message-transport", havingValue = "Kafka")
public class KafkaNotificationService implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaNotificationService.class);

    private final KafkaTemplate<String, LinkUpdateRequest> kafkaTemplate;
    private final String notificationTopic;
    private final String dlqTopic;

    @Autowired
    public KafkaNotificationService(
            KafkaTemplate<String, LinkUpdateRequest> kafkaTemplate,
            @Value("${app.kafka.topics.notifications}") String notificationTopic,
            @Value("${app.kafka.topics.dlq}") String dlqTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.notificationTopic = notificationTopic;
        this.dlqTopic = dlqTopic;

        LOGGER.info(
                "KafkaNotificationService initialized with notificationTopic: {}, dlqTopic: {}",
                notificationTopic,
                dlqTopic);
    }

    @Override
    public Mono<Void> sendNotification(LinkUpdateRequest update) {
        return Mono.fromFuture(kafkaTemplate.send(notificationTopic, update))
                .doOnSuccess(result -> LOGGER.info("Sent message to Kafka topic {}: {}", notificationTopic, update))
                .doOnError(e -> {
                    kafkaTemplate.send(dlqTopic, update);
                    LOGGER.error(
                            "Failed to send message to Kafka topic {}. Sent to DLQ topic {}: {}",
                            notificationTopic,
                            dlqTopic,
                            update,
                            e);
                })
                .then();
    }
}
