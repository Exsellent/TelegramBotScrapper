package backend.academy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.academy.bot.insidebot.TelegramBotService;
import backend.academy.bot.service.NotificationBatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class NotificationBatchServiceTest {

    @Container
    private static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:7.0")).withExposedPorts(6379);

    private NotificationBatchService service;
    private TelegramBotService telegramBotService;
    private RedisTemplate<String, String> redisTemplate;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startContainers() {
        redisContainer.start();
    }

    @BeforeEach
    void setup() {
        // Настройка Redis
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisContainer.getHost());
        redisConfig.setPort(redisContainer.getFirstMappedPort());
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfig);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();

        telegramBotService = mock(TelegramBotService.class);
        objectMapper = new ObjectMapper();
        service = new NotificationBatchService(telegramBotService, redisTemplate, objectMapper);
    }

    @Test
    void testSendDailyDigestWithNotifications() {
        // Подготовка данных
        String key = "notifications:123";
        String message = "{\"tgChatIds\": [123], \"description\": \"Update available\"}";
        redisTemplate.opsForList().rightPush(key, message);

        // Вызов метода
        service.sendDailyDigest();

        // Проверка
        verify(telegramBotService).sendChatMessage(123L, "📬 Daily Digest:\n\nUpdate available\n\n");
        assertEquals(0, redisTemplate.opsForList().size(key), "Redis key should be cleared");
    }

    @Test
    void testSendDailyDigestEmpty() {
        // Пустой Redis

        // Вызов метода
        service.sendDailyDigest();

        // Проверка
        verify(telegramBotService, never()).sendChatMessage(anyLong(), anyString());
    }

    @Test
    void testSendDailyDigestInvalidJson() {
        // Подготовка данных
        String key = "notifications:123";
        String invalidMessage = "invalid json";
        redisTemplate.opsForList().rightPush(key, invalidMessage);

        // Вызов метода
        service.sendDailyDigest();

        // Проверка
        verify(telegramBotService, never()).sendChatMessage(anyLong(), anyString());
        assertEquals(0, redisTemplate.opsForList().size(key), "Redis key should be cleared");
    }
}
