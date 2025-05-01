package backend.academy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.command.ListCommand;
import backend.academy.bot.dto.LinkResponse;
import backend.academy.bot.service.KafkaMessageConsumer;
import backend.academy.bot.service.RedisCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class KafkaMessageConsumerTest {

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private ListCommand listCommand;

    @Mock
    private ObjectMapper objectMapper;

    private KafkaMessageConsumer consumer;

    private Map<Long, CompletableFuture<List<LinkResponse>>> pendingRequests;
    private Long chatId;

    @BeforeEach
    void setUp() {
        redisCacheService = mock(RedisCacheService.class);
        listCommand = mock(ListCommand.class);
        objectMapper = mock(ObjectMapper.class);

        chatId = 123L;
        pendingRequests = new ConcurrentHashMap<>();
        when(listCommand.getPendingRequests()).thenReturn(pendingRequests);

        consumer = new KafkaMessageConsumer(redisCacheService, listCommand, objectMapper);
    }

    @Test
    void testListenWithListCommand() throws Exception {
        String message = "{\"command\": \"list\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("link-commands", 0, 0, chatId.toString(), message);
        CompletableFuture<List<LinkResponse>> future = new CompletableFuture<>();
        pendingRequests.put(chatId, future);

        when(objectMapper.readValue(message, Map.class)).thenReturn(Map.of("command", "list"));

        consumer.listen(record);

        verify(redisCacheService).setLinks(eq(chatId), eq(Collections.emptyList()));
        assertTrue(future.isDone());
        assertEquals(Collections.emptyList(), future.get());
        assertTrue(pendingRequests.isEmpty());
    }

    @Test
    void testListenWithAddCommand() throws Exception {
        String message = "{\"command\": \"add\", \"link\": \"https://example.com\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("link-commands", 0, 0, chatId.toString(), message);
        CompletableFuture<List<LinkResponse>> future = new CompletableFuture<>();
        pendingRequests.put(chatId, future);

        when(objectMapper.readValue(message, Map.class))
                .thenReturn(Map.of("command", "add", "link", "https://example.com"));

        consumer.listen(record);

        verify(redisCacheService).setLinks(eq(chatId), eq(Collections.emptyList()));
        assertTrue(future.isDone());
        assertEquals(Collections.emptyList(), future.get());
        assertTrue(pendingRequests.isEmpty());
    }

    @Test
    void testListenWithInvalidJson() throws Exception {
        String message = "not a json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("link-commands", 0, 0, chatId.toString(), message);
        CompletableFuture<List<LinkResponse>> future = new CompletableFuture<>();
        pendingRequests.put(chatId, future);

        when(objectMapper.readValue(message, Map.class)).thenThrow(new RuntimeException("Invalid JSON"));

        consumer.listen(record);

        verify(redisCacheService).setLinks(eq(chatId), eq(Collections.emptyList()));
        assertTrue(future.isDone());
        assertEquals(Collections.emptyList(), future.get());
        assertTrue(pendingRequests.isEmpty());
    }

    @Test
    void testListenWithUnknownCommand() throws Exception {
        String message = "{\"command\": \"ping\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("link-commands", 0, 0, chatId.toString(), message);
        CompletableFuture<List<LinkResponse>> future = new CompletableFuture<>();
        pendingRequests.put(chatId, future);

        when(objectMapper.readValue(message, Map.class)).thenReturn(Map.of("command", "ping"));

        consumer.listen(record);

        verify(redisCacheService).setLinks(eq(chatId), eq(Collections.emptyList()));
        assertTrue(future.isDone());
        assertEquals(Collections.emptyList(), future.get());
        assertTrue(pendingRequests.isEmpty());
    }
}
