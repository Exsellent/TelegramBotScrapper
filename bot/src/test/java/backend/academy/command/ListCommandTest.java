package backend.academy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.client.ScrapperApiClient;
import backend.academy.bot.command.ListCommand;
import backend.academy.bot.dto.LinkResponse;
import backend.academy.bot.dto.ListLinksResponse;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ListCommandTest {

    @Mock
    private ScrapperApiClient scrapperApiClient;

    @InjectMocks
    private ListCommand command;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testListCommandWithEmptyList() throws Exception {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
        when(scrapperApiClient.getAllLinks(anyLong())).thenReturn(new ListLinksResponse(Collections.emptyList(), 0));

        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");

        // Исправленный текст
        assertEquals("The list of tracked links is empty.", text);
    }

    @Test
    void testListCommandWithNonEmptyList() throws Exception {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        LinkResponse linkResponse = new LinkResponse(1L, "https://example.com", "Description");
        ListLinksResponse listLinksResponse = new ListLinksResponse(Collections.singletonList(linkResponse), 1);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
        when(scrapperApiClient.getAllLinks(anyLong())).thenReturn(listLinksResponse);

        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");

        assertTrue(text.contains("https://example.com"));
    }
}
