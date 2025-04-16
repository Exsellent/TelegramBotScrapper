package backend.academy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.command.ListCommand;
import backend.academy.bot.dto.LinkResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ListCommandTest {

    @Container
    private static final KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Container
    private static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:7.0")).withExposedPorts(6379);

    private ListCommand command;
    private KafkaTemplate<String, String> kafkaTemplate;
    private RedisTemplate<String, List<LinkResponse>> redisTemplate;
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
        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));

        // Настройка Redis
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisContainer.getHost());
        redisConfig.setPort(redisContainer.getFirstMappedPort());
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfig);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // Настройка сериализатора для List<LinkResponse>
        objectMapper = new ObjectMapper();
        Jackson2JsonRedisSerializer<List<LinkResponse>> serializer = new Jackson2JsonRedisSerializer<>(
                objectMapper.getTypeFactory().constructCollectionType(List.class, LinkResponse.class));
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.afterPropertiesSet();

        command = new ListCommand(kafkaTemplate, redisTemplate, objectMapper, "link-commands");
    }

    @Test
    void testListCommandWithEmptyCacheAndEmptyLinks() throws InterruptedException {
        Update update = createUpdate(123L, "/list");

        // Пустой кэш
        assert redisTemplate.opsForValue().get("tracked-links:123") == null;

        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");

        assertNotNull(response);
        assertEquals("The list of tracked links is empty.", text);
    }

    @Test
    void testListCommandWithEmptyCacheAndNonEmptyLinks() throws InterruptedException {
        Update update = createUpdate(123L, "/list");
        LinkResponse link = new LinkResponse(1L, "https://example.com", "Description");
        List<LinkResponse> links = Collections.singletonList(link);

        // Симулируем ответ Kafka
        redisTemplate.opsForValue().set("tracked-links:123", links);

        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");

        assertNotNull(response);
        assertEquals("Tracked links:\nhttps://example.com\n", text);
    }

    @Test
    void testListCommandWithCachedLinks() {
        Update update = createUpdate(123L, "/list");
        LinkResponse link = new LinkResponse(1L, "https://example.com", "Description");
        List<LinkResponse> cachedLinks = Collections.singletonList(link);

        redisTemplate.opsForValue().set("tracked-links:123", cachedLinks);

        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");

        assertNotNull(response);
        assertEquals("Tracked links:\nhttps://example.com\n", text);
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
}
