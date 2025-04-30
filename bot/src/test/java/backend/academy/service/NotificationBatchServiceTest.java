package backend.academy.service;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.insidebot.TelegramBotService;
import backend.academy.bot.service.NotificationBatchService;
import backend.academy.bot.service.RedisCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class NotificationBatchServiceTest {

    @Mock
    private TelegramBotService telegramBotService;

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationBatchService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSendDailyDigestWithNotifications() throws Exception {
        Long chatId = 123L;
        String key = "notifications:123";
        String message = "{\"description\": \"Update available\"}";

        var rootNode = mock(com.fasterxml.jackson.databind.JsonNode.class);
        var descNode = mock(com.fasterxml.jackson.databind.JsonNode.class);

        when(redisCacheService.getNotificationKeys()).thenReturn(Set.of(key));
        when(redisCacheService.getNotifications(chatId)).thenReturn(Collections.singletonList(message));
        when(objectMapper.readTree(message)).thenReturn(rootNode);
        when(rootNode.get("description")).thenReturn(descNode);
        when(descNode.asText()).thenReturn("Update available");

        service.sendDailyDigest();

        verify(telegramBotService).sendChatMessage(chatId, "📬 Daily Digest:\n\nUpdate available\n\n");
        verify(redisCacheService).deleteNotifications(chatId);
    }

    @Test
    void testSendDailyDigestEmpty() {
        // Пустой Redis
        when(redisCacheService.getNotificationKeys()).thenReturn(Collections.emptySet());

        // Вызов
        service.sendDailyDigest();

        // Проверка
        verify(telegramBotService, never()).sendChatMessage(anyLong(), anyString());
    }

    @Test
    void testSendDailyDigestInvalidJson() throws Exception {
        Long chatId = 123L;
        String key = "notifications:123";
        String invalidMessage = "invalid json";

        when(redisCacheService.getNotificationKeys()).thenReturn(Set.of(key));
        when(redisCacheService.getNotifications(chatId)).thenReturn(Collections.singletonList(invalidMessage));
        when(objectMapper.readTree(invalidMessage))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Invalid JSON") {});

        service.sendDailyDigest();

        verify(telegramBotService, never()).sendChatMessage(anyLong(), anyString());
        verify(redisCacheService).deleteNotifications(chatId);
    }
}
