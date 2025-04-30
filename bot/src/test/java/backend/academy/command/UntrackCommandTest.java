package backend.academy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.command.UntrackCommand;
import backend.academy.bot.service.KafkaService;
import backend.academy.bot.service.RedisCacheService;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class UntrackCommandTest {

    @Mock
    private KafkaService kafkaService;

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private UntrackCommand command;

    private Long chatId;
    private Update update;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        chatId = 123L;
    }

    @Test
    void testUntrackCommandMissingUrl() {
        update = createUpdate(chatId, "/untrack");
        SendMessage response = command.handle(update);
        assertNotNull(response);
        assertEquals(
                "Enter the correct URL to delete (e.g., /untrack https://example.com).",
                response.getParameters().get("text"));
    }

    @Test
    void testUntrackCommandInvalidUrl() {
        update = createUpdate(chatId, "/untrack invalid-url");
        SendMessage response = command.handle(update);
        assertNotNull(response);
        assertEquals(
                "Enter the correct URL to delete (e.g., /untrack https://example.com).",
                response.getParameters().get("text"));
    }

    @Test
    void testUntrackCommandSuccess() {
        update = createUpdate(chatId, "/untrack https://example.com");
        SendMessage response = command.handle(update);
        assertNotNull(response);
        assertEquals(
                "Link removal request sent: https://example.com",
                response.getParameters().get("text"));
        verify(kafkaService).sendCommand(eq(chatId), eq("remove"), any());
        verify(redisCacheService).removeLink(chatId, "https://example.com");
    }

    @Test
    void testUntrackCommandKafkaError() {
        update = createUpdate(chatId, "/untrack https://example.com");
        doThrow(new RuntimeException("Kafka error")).when(kafkaService).sendCommand(eq(chatId), eq("remove"), any());
        SendMessage response = command.handle(update);
        assertNotNull(response);
        assertEquals(
                "Error sending link removal request.", response.getParameters().get("text"));
        verify(redisCacheService, never()).removeLink(chatId, "https://example.com");
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
