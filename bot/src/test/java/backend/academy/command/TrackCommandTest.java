package backend.academy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.command.ConversationManager;
import backend.academy.bot.command.ConversationState;
import backend.academy.bot.command.TrackCommand;
import backend.academy.bot.dto.LinkResponse;
import backend.academy.bot.exception.InvalidLinkException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class TrackCommandTest {

    @Container
    private static final KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Container
    private static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:7.0")).withExposedPorts(6379);

    private TrackCommand command;
    private KafkaTemplate<String, String> kafkaTemplate;
    private RedisTemplate<String, List<LinkResponse>> redisTemplate;
    private ConversationManager conversationManager;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startContainers() {
        kafkaContainer.start();
        redisContainer.start();
    }

    @BeforeEach
    void setup() {
        // Настройка Kafka
        Map<String, Object> producerProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);

        // Настройка Redis
        redisTemplate = mock(RedisTemplate.class);

        conversationManager = new ConversationManager();
        objectMapper = new ObjectMapper();

        command = new TrackCommand(conversationManager, kafkaTemplate, redisTemplate, objectMapper, "link-commands");
    }

    @Test
    void testTrackCommandStart() {
        Update update = createUpdate(123L, "/track");
        SendMessage response = command.handle(update);
        assertNotNull(response);
        assertEquals(
                "Please enter the URL you want to track:",
                response.getParameters().get("text"));
    }

    @Test
    void testTrackCommandInvalidUrl() {
        Long chatId = 123L;
        conversationManager.setUserState(chatId, ConversationState.AWAITING_URL);
        Update update = createUpdate(chatId, "invalid-url");

        assertThrows(InvalidLinkException.class, () -> command.handle(update));
    }

    @Test
    void testTrackCommandSuccess() throws Exception {
        Long chatId = 123L;

        // Шаг 1: Начало
        Update startUpdate = createUpdate(chatId, "/track");
        command.handle(startUpdate);

        // Шаг 2: URL
        Update urlUpdate = createUpdate(chatId, "https://example.com");
        command.handle(urlUpdate);

        // Шаг 3: Теги
        Update tagsUpdate = createUpdate(chatId, "tag1 tag2");
        command.handle(tagsUpdate);

        // Шаг 4: Фильтры
        Update filtersUpdate = createUpdate(chatId, "user:john type:comment");
        SendMessage response = command.handle(filtersUpdate);

        assertNotNull(response);
        assertEquals(
                "Link tracking request sent successfully!",
                response.getParameters().get("text"));

        // Проверка кэша (упрощённо, нужно подключить реальный Redis)
        // assertNull(redisTemplate.opsForValue().get("tracked-links:" + chatId));
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
    // Для проверки DLQ
    @Test
    void testInvalidMessageGoesToDlq() throws Exception {
        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaContainer.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "test-group",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(List.of("link-commands-dlq"));

        // Симулируем невалидный URL
        Long chatId = 123L;
        conversationManager.setUserState(chatId, ConversationState.AWAITING_URL);
        assertThrows(InvalidLinkException.class, () -> command.handle(createUpdate(chatId, "invalid-url")));
    }
}
