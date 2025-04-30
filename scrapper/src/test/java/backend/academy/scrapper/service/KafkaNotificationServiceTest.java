package backend.academy.scrapper.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.BotApiClient;
import backend.academy.scrapper.dto.LinkUpdateRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class KafkaNotificationServiceTest {

    private KafkaService kafkaService;
    private BotApiClient botApiClient;
    private KafkaNotificationService service;

    @BeforeEach
    void setup() {
        kafkaService = mock(KafkaService.class);
        botApiClient = mock(BotApiClient.class);
        service = new KafkaNotificationService(kafkaService, botApiClient, "notifications");
    }

    @Test
    void testSendNotificationSuccess() {
        LinkUpdateRequest update =
                new LinkUpdateRequest(1L, "https://example.com", "Update", "Some content", List.of(123L));

        when(kafkaService.sendNotification("notifications", update)).thenReturn(Mono.empty());

        StepVerifier.create(service.sendNotification(update)).verifyComplete();

        verify(kafkaService).sendNotification("notifications", update);
        verify(botApiClient, never()).postUpdate(any());
    }

    @Test
    void testSendNotificationFallbackToHttpOnError() {
        LinkUpdateRequest update =
                new LinkUpdateRequest(1L, "https://example.com", "Update", "Some content", List.of(123L));

        when(kafkaService.sendNotification("notifications", update))
                .thenReturn(Mono.error(new RuntimeException("Kafka error")));
        when(botApiClient.postUpdate(update)).thenReturn(Mono.empty());

        StepVerifier.create(service.sendNotification(update)).verifyComplete();

        verify(kafkaService).sendNotification("notifications", update);
        verify(botApiClient).postUpdate(update);
    }

    @Test
    void testSendNotificationHttpFallbackFails() {
        LinkUpdateRequest update =
                new LinkUpdateRequest(1L, "https://example.com", "Update", "Some content", List.of(123L));

        when(kafkaService.sendNotification("notifications", update))
                .thenReturn(Mono.error(new RuntimeException("Kafka error")));
        when(botApiClient.postUpdate(update)).thenReturn(Mono.error(new RuntimeException("HTTP error")));

        StepVerifier.create(service.sendNotification(update)).verifyComplete();

        verify(kafkaService).sendNotification("notifications", update);
        verify(botApiClient).postUpdate(update);
    }
}
