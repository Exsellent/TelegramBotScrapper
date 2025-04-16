package backend.academy.scrapper.jpa.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.database.jpa.service.JpaChatService;
import backend.academy.scrapper.domain.Chat;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.repository.repository.ChatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JpaChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @InjectMocks
    private JpaChatService chatService;

    private final long testChatId = 1L;

    @Test
    void register_CreatesNewChat_WhenChatDoesNotExist() {
        when(chatRepository.existsById(testChatId)).thenReturn(false);
        when(chatRepository.save(any(Chat.class))).thenReturn(new Chat());

        chatService.register(testChatId);

        verify(chatRepository).existsById(testChatId);
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void unregister_ThrowsChatNotFoundException_WhenChatDoesNotExist() {
        when(chatRepository.existsById(testChatId)).thenReturn(false);

        Exception exception = assertThrows(ChatNotFoundException.class, () -> chatService.unregister(testChatId));

        assertTrue(exception.getMessage().contains("Chat with ID " + testChatId + " not found."));
        verify(chatRepository).existsById(testChatId);
        verify(chatRepository, never()).deleteById(anyLong());
    }
}
