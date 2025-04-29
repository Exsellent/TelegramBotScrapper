package backend.academy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.command.UntrackCommand;
import backend.academy.bot.dto.LinkResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class UntrackCommandTest {

    @Container
    private static final KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    private UntrackCommand command;
    private KafkaTemplate<String, String> kafkaTemplate;
    private RedisTemplate<String, List<LinkResponse>> redisTemplate;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startContainers() {
        kafkaContainer.start();
    }

    @BeforeEach
    void setup() {
        // Настройка Kafka
        Map<String, Object> producerProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));

        // Redis
        redisTemplate = mock(RedisTemplate.class);
        objectMapper = new ObjectMapper();

        command = new UntrackCommand(kafkaTemplate, redisTemplate, objectMapper, "link-commands");
    }

    @Test
    void testUntrackCommandResponseSuccess() throws Exception {
        Update update = createUpdate(123L, "/untrack https://example.com");
        SendMessage response = command.handle(update);

        assertNotNull(response);
        assertEquals(
                "Link removal request sent: https://example.com",
                response.getParameters().get("text"));
    }

    @Test
    void testUntrackCommandNoUrlProvided() {
        Update update = createUpdate(123L, "/untrack");
        SendMessage response = command.handle(update);

        assertNotNull(response);
        assertEquals(
                "Enter the correct URL to delete (e.g., /untrack https://example.com).",
                response.getParameters().get("text"));
    }

    private Update createUpdate(Long chatId, String text) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.text()).thenReturn(text);

        return update;
    }

    @Test
    void testUntrackCommandInvalidJsonSerialization() throws Exception {
        // Arrange
        Update update = createUpdate(123L, "/untrack https://example.com");

        // Мокаем поведение objectMapper — выбрасываем ошибку сериализации
        ObjectMapper mapperMock = mock(ObjectMapper.class);
        when(mapperMock.writeValueAsString(any())).thenThrow(new JsonProcessingException("Invalid JSON") {});

        // Используем замоканную версию objectMapper
        UntrackCommand commandWithBrokenMapper =
                new UntrackCommand(kafkaTemplate, redisTemplate, mapperMock, "link-commands");

        // Act
        SendMessage response = commandWithBrokenMapper.handle(update);

        // Assert
        assertNotNull(response);
        assertEquals(
                "Error sending link removal request.", response.getParameters().get("text"));
    }
}
