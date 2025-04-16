package backend.academy.scrapper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.dto.LinkUpdateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
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
class KafkaNotificationServiceTest {

    @Container
    private static final KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    private KafkaNotificationService service;
    private KafkaTemplate<String, String> kafkaTemplate;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startContainers() {
        kafkaContainer.start();

        // Создание топиков
        try (AdminClient adminClient =
                AdminClient.create(Map.of("bootstrap.servers", kafkaContainer.getBootstrapServers()))) {
            adminClient
                    .createTopics(
                            List.of(new NewTopic("notifications", 1, (short) 1), new NewTopic("dlq", 1, (short) 1)))
                    .all()
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Kafka topics", e);
        }
    }

    @BeforeEach
    void setup() {
        kafkaTemplate = mock(KafkaTemplate.class);
        objectMapper = mock(ObjectMapper.class);
        service = new KafkaNotificationService(kafkaTemplate, objectMapper, "notifications", "dlq");
    }

    @Test
    void testSendNotificationSuccess() throws Exception {
        LinkUpdateRequest update = new LinkUpdateRequest(1L, "https://example.com", "Update", "content", List.of(123L));
        String json =
                "{\"id\":1,\"url\":\"https://example.com\",\"description\":\"Update\",\"updateType\":\"content\",\"tgChatIds\":[123]}";

        when(objectMapper.writeValueAsString(update)).thenReturn(json);

        service.sendNotification(update);

        verify(kafkaTemplate).send("notifications", json);
    }

    @Test
    void testSendNotificationToDlqOnError() throws Exception {
        LinkUpdateRequest update = new LinkUpdateRequest(1L, "https://example.com", "Update", "content", List.of(123L));

        when(objectMapper.writeValueAsString(update)).thenThrow(new JsonProcessingException("Serialization error") {});

        service.sendNotification(update);

        verify(kafkaTemplate).send("dlq", update.toString());
    }

    @Test
    void testDlqMessageContent() throws Exception {
        // Для этого теста используем реальный KafkaTemplate
        Map<String, Object> producerProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        KafkaTemplate<String, String> realKafkaTemplate =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
        KafkaNotificationService realService =
                new KafkaNotificationService(realKafkaTemplate, objectMapper, "notifications", "dlq");

        LinkUpdateRequest update = new LinkUpdateRequest(1L, "https://example.com", "Update", "content", List.of(123L));
        when(objectMapper.writeValueAsString(update)).thenThrow(new JsonProcessingException("Serialization error") {});

        realService.sendNotification(update);

        // Настройка KafkaConsumer
        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaContainer.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "test-group",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(List.of("dlq"));

        // Ждём сообщения
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        consumer.close();

        // Проверка
        boolean messageFound = false;
        for (ConsumerRecord<String, String> record : records) {
            if (record.value().equals(update.toString())) {
                messageFound = true;
                break;
            }
        }
        assertEquals(true, messageFound, "DLQ should contain the expected message");
    }
}
