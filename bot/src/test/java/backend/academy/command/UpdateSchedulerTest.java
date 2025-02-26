package backend.academy.command;

import backend.academy.bot.client.ScrapperApiClient;
import backend.academy.bot.command.UpdateScheduler;
import backend.academy.bot.dto.LinkUpdateRequest;
import backend.academy.bot.insidebot.TelegramBotService;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateSchedulerTest {

    private ScrapperApiClient scrapperApiClient;
    private TelegramBotService bot;
    private UpdateScheduler updateScheduler;

    @BeforeEach
    void setUp() {
        scrapperApiClient = mock(ScrapperApiClient.class);
        bot = mock(TelegramBotService.class);
        updateScheduler = new UpdateScheduler(scrapperApiClient, bot);
    }

    @Test
    void givenUsersFollowLink_whenUpdateOccurs_thenSendMessageToOnlySubscribedUsers() {
        // Подготовка данных
        LinkUpdateRequest update1 = new LinkUpdateRequest(null, // id (можно передать любое число, если оно обязательно)
            "https://github.com/test/repo1", "New commit added", null, // updateType
            List.of(111L, 222L) // Только эти чаты подписаны
        );

        LinkUpdateRequest update2 = new LinkUpdateRequest(null, "https://stackoverflow.com/q/12345", "New answer posted", null, List.of(333L));


        when(scrapperApiClient.getUpdates()).thenReturn(List.of(update1, update2));

        updateScheduler.checkUpdates();

        // Захват отправленных сообщений
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot, times(3)).execute(messageCaptor.capture());

        // Проверяем, что сообщения отправлены только подписанным пользователям
        List<SendMessage> sentMessages = messageCaptor.getAllValues();
        assertEquals(3, sentMessages.size());

        assertEquals(111L, sentMessages.get(0).getParameters().get("chat_id"));
        assertEquals("Detected changes in https://github.com/test/repo1:\nNew commit added", sentMessages.get(0).getParameters().get("text"));

        assertEquals(222L, sentMessages.get(1).getParameters().get("chat_id"));
        assertEquals("Detected changes in https://github.com/test/repo1:\nNew commit added", sentMessages.get(1).getParameters().get("text"));

        assertEquals(333L, sentMessages.get(2).getParameters().get("chat_id"));
        assertEquals("Detected changes in https://stackoverflow.com/q/12345:\nNew answer posted", sentMessages.get(2).getParameters().get("text"));
    }

    @Test
    void givenNoUpdates_whenCheckUpdatesRuns_thenNoMessagesAreSent() {
        when(scrapperApiClient.getUpdates()).thenReturn(Collections.emptyList());

        updateScheduler.checkUpdates();

        verify(bot, never()).execute(any(SendMessage.class));
    }
}
