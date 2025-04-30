package backend.academy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.command.ListCommand;
import backend.academy.bot.dto.LinkResponse;
import backend.academy.bot.service.KafkaMessageConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class KafkaMessageConsumerTest {

    private RedisTemplate<String, List<LinkResponse>> redisTemplate;
    private ValueOperations<String, List<LinkResponse>> valueOperations;
    private ListCommand listCommand;
    private ObjectMapper objectMapper;
    private KafkaMessageConsumer consumer;

    private final Long chatId = 123L;

    @BeforeEach
    void setup() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        listCommand = mock(ListCommand.class);
        objectMapper = new ObjectMapper();

        consumer = new KafkaMessageConsumer(redisTemplate, listCommand, objectMapper);
    }

    @Test
    void testKafkaMessageProcessedCorrectly() throws Exception {
        // Подготовка
        CompletableFuture<List<LinkResponse>> future = new CompletableFuture<>();
        Map<Long, CompletableFuture<List<LinkResponse>>> pendingRequests = new HashMap<>();
        pendingRequests.put(chatId, future);

        when(listCommand.getPendingRequests()).thenReturn(pendingRequests);

        String json = """
                {
                  "command": "list"
                }
                """;

        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, chatId.toString(), json);

        // Создание consumer вручную (с нужными pendingRequests)
        consumer = new KafkaMessageConsumer(redisTemplate, listCommand, objectMapper);

        // Акт
        consumer.listen(record);

        // Проверка записи в Redis
        verify(redisTemplate.opsForValue(), times(1))
                .set(eq("tracked-links:" + chatId), eq(Collections.emptyList()), eq(10L), eq(TimeUnit.MINUTES));

        // Проверка завершения CompletableFuture
        assertTrue(future.isDone());
        assertEquals(Collections.emptyList(), future.get());
    }

    @Test
    void testInvalidJsonHandledGracefully() {
        CompletableFuture<List<LinkResponse>> future = new CompletableFuture<>();
        Map<Long, CompletableFuture<List<LinkResponse>>> pendingRequests = new HashMap<>();
        pendingRequests.put(chatId, future);

        when(listCommand.getPendingRequests()).thenReturn(pendingRequests);

        String invalidJson = "{ this is not valid json }";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, chatId.toString(), invalidJson);

        consumer = new KafkaMessageConsumer(redisTemplate, listCommand, objectMapper);

        consumer.listen(record);

        // смотрим, что при ошибке парсинга всё равно завершается пустым списком
        assertTrue(future.isDone());
        assertEquals(Collections.emptyList(), future.join());
    }
}
