package backend.academy.scrapper.jdbc.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.database.jdbc.service.JdbcChatService;
import backend.academy.scrapper.dto.ChatDTO;
import backend.academy.scrapper.exception.ChatAlreadyRegisteredException;
import backend.academy.scrapper.exception.ChatNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JdbcChatServiceTest {

    @Mock
    private ChatDao chatDao;

    @InjectMocks
    private JdbcChatService chatService;

    private final long testChatId = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(chatDao.existsById(testChatId)).thenReturn(false);
    }

    @Test
    void register_CreatesNewChat_WhenChatDoesNotExist() {
        doNothing().when(chatDao).add(any(ChatDTO.class));

        chatService.register(testChatId);

        verify(chatDao).existsById(testChatId);
        verify(chatDao).add(any(ChatDTO.class));
    }

    @Test
    void register_ThrowsChatAlreadyRegisteredException_WhenChatExists() {
        when(chatDao.existsById(testChatId)).thenReturn(true);

        Exception exception =
                assertThrows(ChatAlreadyRegisteredException.class, () -> chatService.register(testChatId));
        assertTrue(exception.getMessage().contains("Chat with id " + testChatId + " already exists."));
        verify(chatDao).existsById(testChatId);
        verify(chatDao, never()).add(any());
    }

    @Test
    void unregister_RemovesChat_WhenChatExists() {
        when(chatDao.existsById(testChatId)).thenReturn(true);
        doNothing().when(chatDao).remove(testChatId);

        chatService.unregister(testChatId);

        verify(chatDao).existsById(testChatId);
        verify(chatDao).remove(testChatId);
    }

    @Test
    void unregister_ThrowsChatNotFoundException_WhenChatDoesNotExist() {
        Exception exception = assertThrows(ChatNotFoundException.class, () -> chatService.unregister(testChatId));
        assertTrue(exception.getMessage().contains("Chat with Id " + testChatId + " not found."));
        verify(chatDao).existsById(testChatId);
        verify(chatDao, never()).remove(anyLong());
    }
}
