package backend.academy.insidebot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.command.CommandHandler;
import backend.academy.bot.insidebot.TelegramBotService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramBotServiceTest {
    @Mock
    private TelegramBot bot;

    @Mock
    private CommandHandler commandHandler;

    @InjectMocks
    private TelegramBotService telegramBotImpl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        telegramBotImpl = new TelegramBotService(commandHandler, bot);
    }

    @Test
    void whenUpdatesReceived_thenProcessMessages() {
        Update update = mock(Update.class);
        List<Update> updates = List.of(update);
        SendMessage sendMessage = new SendMessage(123L, "Test Message");

        when(commandHandler.handleUpdate(update)).thenReturn(sendMessage);

        // Убрана заглушка для bot.execute()
        int result = telegramBotImpl.process(updates);

        // Проверяем, что execute был вызван
        verify(bot).execute(sendMessage);
        assertEquals(UpdatesListener.CONFIRMED_UPDATES_ALL, result);
    }

    @Test
    void whenNoUpdatesReceived_thenNothingHappens() {
        List<Update> updates = List.of();
        int result = telegramBotImpl.process(updates);

        verify(bot, never()).execute(any());
        assertEquals(UpdatesListener.CONFIRMED_UPDATES_ALL, result);
    }
}
