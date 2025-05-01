package backend.academy.scrapper.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.dto.LinkDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class KafkaLinkCommandConsumerTest {

    @Container
    private static final KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    private KafkaLinkCommandConsumer consumer;
    private LinkService linkService;
    private ChatLinkService chatLinkService;
    private ObjectMapper objectMapper;
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeAll
    static void startContainers() {
        kafkaContainer.start();
    }

    @BeforeEach
    void setup() {
        linkService = mock(LinkService.class);
        chatLinkService = mock(ChatLinkService.class);
        objectMapper = new ObjectMapper();

        consumer = new KafkaLinkCommandConsumer(linkService, chatLinkService, objectMapper);

        // Настройка Kafka
        Map<String, Object> producerProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                        org.apache.kafka.common.serialization.StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                        org.apache.kafka.common.serialization.StringSerializer.class);
        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
    }

    @Test
    void testAddCommand() throws Exception {
        String message = "{\"command\": \"add\", \"link\": \"https://example.com\", \"filters\": {\"tag\": \"test\"}}";
        String chatId = "123";

        LinkDTO linkDTO = LinkDTO.builder()
                .linkId(1L)
                .url("https://example.com")
                .description("Added via Kafka")
                .build();

        when(linkService.add("https://example.com", "Added via Kafka")).thenReturn(linkDTO);

        consumer.processCommand(message, chatId);

        verify(linkService).add("https://example.com", "Added via Kafka");
        verify(chatLinkService).addLinkToChat(123L, 1L, Map.of("tag", "test"));
    }

    @Test
    void testRemoveCommand() throws Exception {
        String message = "{\"command\": \"remove\", \"link\": \"https://example.com\", \"filters\": {}}";
        String chatId = "123";

        LinkDTO linkDTO =
                LinkDTO.builder().linkId(1L).url("https://example.com").build();

        when(linkService.findByUrl("https://example.com")).thenReturn(linkDTO);

        consumer.processCommand(message, chatId);

        verify(linkService).findByUrl("https://example.com");
        verify(chatLinkService).removeLinkFromChat(123L, 1L);
    }

    @Test
    void testRemoveCommandLinkNotFound() throws Exception {
        String message = "{\"command\": \"remove\", \"link\": \"https://example.com\", \"filters\": {}}";
        String chatId = "123";

        when(linkService.findByUrl("https://example.com")).thenReturn(null);

        consumer.processCommand(message, chatId);

        verify(linkService).findByUrl("https://example.com");
        verify(chatLinkService, never()).removeLinkFromChat(anyLong(), anyLong());
    }

    @Test
    void testInvalidJson() {
        String message = "invalid json";
        String chatId = "123";

        consumer.processCommand(message, chatId);

        verify(linkService, never()).add(any(), any());
        verify(chatLinkService, never()).addLinkToChat(anyLong(), anyLong(), any());
    }

    @Test
    void testUnknownCommand() throws Exception {
        String message = "{\"command\": \"unknown\", \"link\": \"https://example.com\", \"filters\": {}}";
        String chatId = "123";

        consumer.processCommand(message, chatId);

        verify(linkService, never()).add(any(), any());
        verify(chatLinkService, never()).addLinkToChat(anyLong(), anyLong(), any());
    }
}
