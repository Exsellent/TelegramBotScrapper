package backend.academy.scrapper.jdbc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.dao.ChatLinkDao;
import backend.academy.scrapper.database.jdbc.service.JdbcChatLinkService;
import backend.academy.scrapper.domain.ChatLink;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.exception.ChatNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JdbcChatLinkServiceTest {

    @Mock
    private ChatDao chatDao;

    @Mock
    private ChatLinkDao chatLinkDao;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JdbcChatLinkService chatLinkService;

    private final long testChatId = 1L;
    private final long testLinkId = 100L;

    @BeforeEach
    void setUp() {
        lenient().when(chatDao.existsById(testChatId)).thenReturn(true);
        lenient().when(chatDao.existsById(999L)).thenReturn(false);
    }

    @Test
    void shouldAddLinkToChatWhenChatExists() {
        ChatLink chatLink = new ChatLink();
        chatLink.setChatId(testChatId);
        chatLink.setLinkId(testLinkId);
        chatLink.setSharedAt(LocalDateTime.now());
        chatLink.setFilters("{}");
        doNothing().when(chatLinkDao).add(any(ChatLink.class));

        chatLinkService.addLinkToChat(testChatId, testLinkId);

        verify(chatLinkDao).add(any(ChatLink.class));
        verify(chatDao).existsById(testChatId);
    }

    @Test
    void shouldThrowExceptionWhenAddingLinkToNonExistentChat() {
        assertThrows(ChatNotFoundException.class, () -> chatLinkService.addLinkToChat(999L, testLinkId));
        verify(chatDao).existsById(999L);
        verify(chatLinkDao, never()).add(any());
    }

    @Test
    void shouldRemoveLinkFromChat() {
        doNothing().when(chatLinkDao).removeByChatIdAndLinkId(testChatId, testLinkId);

        chatLinkService.removeLinkFromChat(testChatId, testLinkId);

        verify(chatLinkDao).removeByChatIdAndLinkId(testChatId, testLinkId);
    }

    @Test
    void shouldFindAllLinksForChat() throws Exception {
        // 1. Подготовка данных
        ChatLink chatLink = new ChatLink();
        chatLink.setChatId(testChatId);
        chatLink.setLinkId(testLinkId);
        chatLink.setSharedAt(LocalDateTime.now());
        chatLink.setFilters("{\"tag\":\"test\"}");

        // 2. Настройка моков DAO
        when(chatLinkDao.findByChatId(testChatId)).thenReturn(List.of(chatLink));

        // 3. Настройка ObjectMapper — обратите внимание на аргументы
        // Используем Class<Map> вместо TypeReference
        doReturn(Map.of("tag", "test")).when(objectMapper).readValue(anyString(), eq(Map.class));

        // 4. Вызов тестируемого метода
        Collection<ChatLinkDTO> result = chatLinkService.findAllLinksForChat(testChatId);

        // 5. Проверки
        assertEquals(1, result.size());
        ChatLinkDTO dto = result.iterator().next();
        assertEquals(Map.of("tag", "test"), dto.getFilters());

        // 6. Верификация вызовов
        verify(objectMapper).readValue(anyString(), eq(Map.class));
    }
}
