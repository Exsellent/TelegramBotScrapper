package backend.academy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.client.ScrapperApiClient;
import backend.academy.bot.command.ConversationManager;
import backend.academy.bot.command.ConversationState;
import backend.academy.bot.command.TrackCommand;
import backend.academy.bot.dto.AddLinkRequest;
import backend.academy.bot.exception.LinkAlreadyAddedException;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class TrackCommandTest {

    @Mock
    private ScrapperApiClient scrapperApiClient;

    @Mock
    private ConversationManager conversationManager;

    @InjectMocks
    private TrackCommand command;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testTrackCommandStart() {
        Update update = createUpdate(123L, "/track");
        when(conversationManager.getUserState(anyLong())).thenReturn(ConversationState.IDLE);

        SendMessage response = command.handle(update);
        assertNotNull(response, "Response should not be null");
        assertEquals(
                "Please enter the URL you want to track:",
                response.getParameters().get("text"));
    }

    @Test
    void testTrackCommandAwaitingUrl() {
        Long chatId = 123L;
        Update update = createUpdate(chatId, "https://example.com");

        when(conversationManager.getUserState(chatId)).thenReturn(ConversationState.AWAITING_URL);
        when(conversationManager.getTrackingData(chatId)).thenReturn(new ConversationManager.TrackingData());

        SendMessage response = command.handle(update);
        assertNotNull(response, "Response should not be null");
        assertEquals(
                "Enter tags for this link (optional, space-separated) or type 'skip':",
                response.getParameters().get("text"));
    }

    @Test
    void testTrackCommandSuccess() throws Exception {
        Long chatId = 123L;
        when(conversationManager.getUserState(chatId)).thenReturn(ConversationState.AWAITING_FILTERS);
        ConversationManager.TrackingData data = new ConversationManager.TrackingData();
        data.setUrl("https://example.com");
        data.setTags(List.of("tag1", "tag2"));
        when(conversationManager.getTrackingData(chatId)).thenReturn(data);
        doNothing().when(scrapperApiClient).addLink(anyLong(), any(AddLinkRequest.class));

        Update update = createUpdate(chatId, "user:john type:comment");
        SendMessage response = command.handle(update);

        assertNotNull(response, "Response should not be null");
        assertEquals(
                "Link successfully added with specified tags and filters!",
                response.getParameters().get("text"));
    }

    @Test
    void testTrackCommandLinkAlreadyTracked() throws Exception {
        Long chatId = 123L;
        when(conversationManager.getUserState(chatId)).thenReturn(ConversationState.AWAITING_FILTERS);
        ConversationManager.TrackingData data = new ConversationManager.TrackingData();
        data.setUrl("https://example.com");
        data.setTags(List.of("tag1"));
        when(conversationManager.getTrackingData(chatId)).thenReturn(data);
        doThrow(new LinkAlreadyAddedException("Link already tracked"))
                .when(scrapperApiClient)
                .addLink(anyLong(), any(AddLinkRequest.class));

        Update update = createUpdate(chatId, "user:alice");
        SendMessage response = command.handle(update);

        assertNotNull(response, "Response should not be null");
        assertEquals(
                "Error adding link. Please try again.", response.getParameters().get("text"));
    }

    // Метод создания моков для Update
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
