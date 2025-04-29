package backend.academy.scrapper.service;

import backend.academy.scrapper.client.BotApiClient;
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
    private final BotApiClient botApiClient;
    private final String notificationTopic;

    @Autowired
    public KafkaNotificationService(
            KafkaTemplate<String, LinkUpdateRequest> kafkaTemplate,
            BotApiClient botApiClient,
            @Value("${app.kafka.topics.notifications}") String notificationTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.botApiClient = botApiClient;
        this.notificationTopic = notificationTopic;

        LOGGER.info("KafkaNotificationService initialized with notificationTopic: {}", notificationTopic);
    }

    @Override
    public Mono<Void> sendNotification(LinkUpdateRequest update) {
        return Mono.fromFuture(kafkaTemplate.send(notificationTopic, update))
                .then()
                .doOnSuccess(v -> LOGGER.info("Sent message to Kafka topic {}: {}", notificationTopic, update))
                .onErrorResume(e -> {
                    LOGGER.error("Kafka failed, falling back to HTTP: {}", e.getMessage());
                    return botApiClient
                            .postUpdate(update)
                            .doOnSuccess(v -> LOGGER.info("Sent message to HTTP endpoint: {}", update))
                            .doOnError(httpError -> LOGGER.error("Failed to send to HTTP: {}", httpError.getMessage()))
                            .onErrorResume(httpError -> Mono.empty());
                });
    }
}
