package backend.academy.scrapper.service;

import static org.mockito.Mockito.*;

import backend.academy.scrapper.client.BotApiClient;
import backend.academy.scrapper.dto.LinkUpdateRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class KafkaNotificationServiceTest {

    private KafkaTemplate<String, LinkUpdateRequest> kafkaTemplate;
    private BotApiClient botApiClient;
    private KafkaNotificationService service;

    @BeforeEach
    void setup() {
        kafkaTemplate = mock(KafkaTemplate.class);
        botApiClient = mock(BotApiClient.class);
        service = new KafkaNotificationService(kafkaTemplate, botApiClient, "notifications");
    }

    @Test
    void testSendNotificationSuccess() {
        LinkUpdateRequest update =
                new LinkUpdateRequest(1L, "https://example.com", "Update", "Some content", List.of(123L));

        when(kafkaTemplate.send("notifications", update)).thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(service.sendNotification(update)).verifyComplete();

        verify(kafkaTemplate).send("notifications", update);
        verify(botApiClient, never()).postUpdate(any());
    }

    @Test
    void testSendNotificationFallbackToHttpOnError() {
        LinkUpdateRequest update =
                new LinkUpdateRequest(1L, "https://example.com", "Update", "Some content", List.of(123L));

        when(kafkaTemplate.send("notifications", update))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka error")));
        when(botApiClient.postUpdate(update)).thenReturn(Mono.empty()); // Успешный HTTP fallback

        StepVerifier.create(service.sendNotification(update)).verifyComplete();

        verify(kafkaTemplate).send("notifications", update);
        verify(botApiClient).postUpdate(update);
    }

    @Test
    void testSendNotificationHttpFallbackFails() {
        LinkUpdateRequest update =
                new LinkUpdateRequest(1L, "https://example.com", "Update", "Some content", List.of(123L));

        when(kafkaTemplate.send("notifications", update))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka error")));
        when(botApiClient.postUpdate(update)).thenReturn(Mono.error(new RuntimeException("HTTP error")));

        StepVerifier.create(service.sendNotification(update)).verifyComplete();

        verify(kafkaTemplate).send("notifications", update);
        verify(botApiClient).postUpdate(update);
    }
}
