package backend.academy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.command.ConversationManager;
import backend.academy.bot.command.ConversationState;
import backend.academy.bot.command.TrackCommand;
import backend.academy.bot.exception.InvalidLinkException;
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

class TrackCommandTest {

    @Mock
    private ConversationManager conversationManager;

    @Mock
    private KafkaService kafkaService;

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private TrackCommand command;

    private Long chatId;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        chatId = 123L;
        when(conversationManager.getUserState(chatId)).thenReturn(ConversationState.IDLE);
        when(conversationManager.getTrackingData(chatId)).thenReturn(new ConversationManager.TrackingData());
    }

    @Test
    void testTrackCommandStart() {
        Update update = createUpdate(chatId, "/track");
        SendMessage response = command.handle(update);
        assertNotNull(response);
        assertEquals(
                "Please enter the URL you want to track:",
                response.getParameters().get("text"));
        verify(conversationManager).setUserState(chatId, ConversationState.AWAITING_URL);
    }

    @Test
    void testTrackCommandInvalidUrl() {
        when(conversationManager.getUserState(chatId)).thenReturn(ConversationState.AWAITING_URL);
        Update update = createUpdate(chatId, "invalid-url");

        assertThrows(InvalidLinkException.class, () -> command.handle(update));
    }

    @Test
    void testTrackCommandSuccess() {
        ConversationManager.TrackingData data = new ConversationManager.TrackingData();
        when(conversationManager.getTrackingData(chatId)).thenReturn(data);

        // Шаг 1: Начало
        Update startUpdate = createUpdate(chatId, "/track");
        command.handle(startUpdate);
        verify(conversationManager).setUserState(chatId, ConversationState.AWAITING_URL);

        // Шаг 2: URL
        when(conversationManager.getUserState(chatId)).thenReturn(ConversationState.AWAITING_URL);
        Update urlUpdate = createUpdate(chatId, "https://example.com");
        command.handle(urlUpdate);
        verify(conversationManager).setUserState(chatId, ConversationState.AWAITING_TAGS);

        // Шаг 3: Теги
        when(conversationManager.getUserState(chatId)).thenReturn(ConversationState.AWAITING_TAGS);
        Update tagsUpdate = createUpdate(chatId, "tag1 tag2");
        command.handle(tagsUpdate);
        verify(conversationManager).setUserState(chatId, ConversationState.AWAITING_FILTERS);

        // Шаг 4: Фильтры
        when(conversationManager.getUserState(chatId)).thenReturn(ConversationState.AWAITING_FILTERS);
        Update filtersUpdate = createUpdate(chatId, "user:john type:comment");
        SendMessage response = command.handle(filtersUpdate);

        assertNotNull(response);
        assertEquals(
                "Link tracking request sent successfully!",
                response.getParameters().get("text"));
        verify(kafkaService).sendCommand(eq(chatId), eq("add"), any());
        verify(redisCacheService).deleteLinks(chatId);
        verify(conversationManager).setUserState(chatId, ConversationState.IDLE);
        verify(conversationManager).clearTrackingData(chatId);
    }

    @Test
    void testTrackCommandSkipTagsAndFilters() {
        ConversationManager.TrackingData data = new ConversationManager.TrackingData();
        when(conversationManager.getTrackingData(chatId)).thenReturn(data);

        // Шаг 1: Начало
        Update startUpdate = createUpdate(chatId, "/track");
        command.handle(startUpdate);

        // Шаг 2: URL
        when(conversationManager.getUserState(chatId)).thenReturn(ConversationState.AWAITING_URL);
        Update urlUpdate = createUpdate(chatId, "https://example.com");
        command.handle(urlUpdate);

        // Шаг 3: Пропуск тегов
        when(conversationManager.getUserState(chatId)).thenReturn(ConversationState.AWAITING_TAGS);
        Update tagsUpdate = createUpdate(chatId, "skip");
        command.handle(tagsUpdate);

        // Шаг 4: Пропуск фильтров
        when(conversationManager.getUserState(chatId)).thenReturn(ConversationState.AWAITING_FILTERS);
        Update filtersUpdate = createUpdate(chatId, "skip");
        SendMessage response = command.handle(filtersUpdate);

        assertNotNull(response);
        assertEquals(
                "Link tracking request sent successfully!",
                response.getParameters().get("text"));
        verify(kafkaService).sendCommand(eq(chatId), eq("add"), any());
        verify(redisCacheService).deleteLinks(chatId);
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
