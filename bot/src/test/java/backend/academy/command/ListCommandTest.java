package backend.academy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.command.ListCommand;
import backend.academy.bot.dto.LinkResponse;
import backend.academy.bot.service.KafkaService;
import backend.academy.bot.service.RedisCacheService;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ListCommandTest {

    @Mock
    private KafkaService kafkaService;

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private ListCommand command;

    private Long chatId;
    private Update update;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        chatId = 123L;
        update = createUpdate(chatId, "/list");
    }

    @Test
    void testListCommandWithEmptyCacheAndEmptyLinks() {
        // Мокаем пустой кэш
        when(redisCacheService.getLinks(chatId)).thenReturn(null);

        // Мокаем ответ от Kafka (пустой список)
        doAnswer(invocation -> {
                    CompletableFuture<List<LinkResponse>> future =
                            command.getPendingRequests().get(chatId);
                    if (future != null) {
                        future.complete(Collections.emptyList());
                    }
                    return null;
                })
                .when(kafkaService)
                .sendCommand(eq(chatId), eq("list"), any());

        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");

        assertNotNull(response);
        assertEquals("The list of tracked links is empty.", text);
    }

    @Test
    void testListCommandWithEmptyCacheAndNonEmptyLinks() {
        // Мокаем пустой кэш
        when(redisCacheService.getLinks(chatId)).thenReturn(null);

        // Мокаем ответ от Kafka (список с одной ссылкой)
        LinkResponse link = new LinkResponse(1L, "https://example.com", "Example link");
        List<LinkResponse> links = Collections.singletonList(link);

        doAnswer(invocation -> {
                    CompletableFuture<List<LinkResponse>> future =
                            command.getPendingRequests().get(chatId);
                    if (future != null) {
                        future.complete(links);
                    }
                    return null;
                })
                .when(kafkaService)
                .sendCommand(eq(chatId), eq("list"), any());

        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");

        assertNotNull(response);
        assertEquals("Tracked links:\nhttps://example.com\n", text);
    }

    @Test
    void testListCommandWithCachedLinks() {
        // Мокаем кэшированные ссылки
        LinkResponse link = new LinkResponse(1L, "https://example.com", "Example link");
        List<LinkResponse> cachedLinks = Collections.singletonList(link);
        when(redisCacheService.getLinks(chatId)).thenReturn(cachedLinks);

        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");

        assertNotNull(response);
        assertEquals("Tracked links:\nhttps://example.com\n", text);
    }

    @Test
    void testListCommandWithKafkaTimeout() {
        // Мокаем пустой кэш
        when(redisCacheService.getLinks(chatId)).thenReturn(null);

        // Мокаем Kafka, который не отвечает (не завершает CompletableFuture)
        doAnswer(invocation -> null).when(kafkaService).sendCommand(eq(chatId), eq("list"), any());

        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");

        assertNotNull(response);
        assertEquals("Timeout while fetching links.", text);
    }

    @Test
    void testListCommandWithKafkaResponse() {
        // Мокаем пустой кэш
        when(redisCacheService.getLinks(chatId)).thenReturn(null);

        // Мокаем ответ от Kafka
        LinkResponse link = new LinkResponse(1L, "https://example.com", "Example link");
        List<LinkResponse> links = Collections.singletonList(link);

        doAnswer(invocation -> {
                    CompletableFuture<List<LinkResponse>> future =
                            command.getPendingRequests().get(chatId);
                    if (future != null) {
                        future.complete(links);
                    }
                    return null;
                })
                .when(kafkaService)
                .sendCommand(eq(chatId), eq("list"), any());

        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");

        assertNotNull(response);
        assertTrue(text.contains("https://example.com"));
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
