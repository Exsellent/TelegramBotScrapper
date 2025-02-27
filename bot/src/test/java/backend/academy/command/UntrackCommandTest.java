package backend.academy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.client.ScrapperApiClient;
import backend.academy.bot.command.UntrackCommand;
import backend.academy.bot.dto.RemoveLinkRequest;
import backend.academy.bot.exception.ChatNotFoundException;
import backend.academy.bot.exception.LinkNotFoundException;
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
    private ScrapperApiClient scrapperApiClient;

    @InjectMocks
    private UntrackCommand command;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUntrackCommandResponseSuccess() throws Exception {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        String urlToUntrack = "https://example.com";
        Long chatId = 123L;

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.text()).thenReturn("/untrack " + urlToUntrack);

        doNothing().when(scrapperApiClient).removeLink(anyLong(), any(RemoveLinkRequest.class));

        SendMessage response = command.handle(update);

        String text = (String) response.getParameters().get("text");
        assertEquals("Link removed from tracking: " + urlToUntrack, text);
    }

    @Test
    void testUntrackCommandLinkNotFoundException() throws Exception {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        String urlToUntrack = "https://example.com";
        Long chatId = 123L;

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.text()).thenReturn("/untrack " + urlToUntrack);

        doThrow(new LinkNotFoundException("Link not found"))
                .when(scrapperApiClient)
                .removeLink(anyLong(), any(RemoveLinkRequest.class));

        SendMessage response = command.handle(update);

        String text = (String) response.getParameters().get("text");
        assertEquals("This link is not tracked.", text);
    }

    @Test
    void testUntrackCommandChatNotFoundException() throws Exception {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        String urlToUntrack = "https://example.com";
        Long chatId = 123L;

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(message.text()).thenReturn("/untrack " + urlToUntrack);

        doThrow(new ChatNotFoundException("Chat not found"))
                .when(scrapperApiClient)
                .removeLink(anyLong(), any(RemoveLinkRequest.class));

        SendMessage response = command.handle(update);

        String text = (String) response.getParameters().get("text");
        assertEquals("First, register using /start.", text);
    }
}
