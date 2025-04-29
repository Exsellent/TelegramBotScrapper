package backend.academy.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.insidebot.TelegramBotService;
import backend.academy.bot.service.KafkaMessageConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class KafkaMessageConsumerTest {

    private TelegramBotService telegramBotService;
    private RedisTemplate<String, String> redisTemplate;
    private ObjectMapper objectMapper;
    private KafkaMessageConsumer consumer;

    @BeforeEach
    void setUp() {
        telegramBotService = mock(TelegramBotService.class);
        redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        objectMapper = new ObjectMapper();
        consumer = new KafkaMessageConsumer(telegramBotService, redisTemplate, objectMapper);
    }

    @Test
    void testInstantNotification() {
        // Redis не возвращает режим — по умолчанию instant
        when(redisTemplate.opsForValue().get("notification-mode:123")).thenReturn(null);

        String message = "{\"tgChatIds\": [123], \"description\": \"Update available\"}";
        consumer.listen(message);

        verify(telegramBotService).sendChatMessage(123L, "Update available");
    }

    @Test
    void testDigestNotification() {
        // Redis возвращает режим digest
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("notification-mode:123")).thenReturn("digest");

        var listOps = mock(org.springframework.data.redis.core.ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);

        String message = "{\"tgChatIds\": [123], \"description\": \"Update available\"}";
        consumer.listen(message);

        verify(listOps).rightPush("notifications:123", message);
    }
}
