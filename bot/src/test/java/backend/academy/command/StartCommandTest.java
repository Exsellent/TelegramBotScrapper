package backend.academy.command;

import backend.academy.bot.client.ScrapperApiClient;
import backend.academy.bot.command.StartCommand;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StartCommandTest {

    @Mock
    private ScrapperApiClient scrapperApiClient;

    @InjectMocks
    private StartCommand command;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testStartCommandResponse() throws Exception {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
        doNothing().when(scrapperApiClient).registerChat(anyLong());

        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");
        assertEquals("Welcome!", text);
    }
}
