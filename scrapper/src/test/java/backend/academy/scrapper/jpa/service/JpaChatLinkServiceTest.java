package backend.academy.scrapper.jpa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.database.jpa.service.JpaChatLinkService;
import backend.academy.scrapper.domain.ChatLink;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.repository.repository.ChatLinkRepository;
import backend.academy.scrapper.repository.repository.ChatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JpaChatLinkServiceTest {

    @Mock
    private ChatLinkRepository chatLinkRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JpaChatLinkService chatLinkService;

    private final long testChatId = 1L;
    private final long testLinkId = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(chatRepository.existsById(testChatId)).thenReturn(true);
    }

    @Test
    void addLinkToChat_ThrowsChatNotFoundException_IfChatDoesNotExist() {
        long nonExistingChatId = -1L;
        when(chatRepository.existsById(nonExistingChatId)).thenReturn(false);

        Exception exception = assertThrows(
                ChatNotFoundException.class, () -> chatLinkService.addLinkToChat(nonExistingChatId, testLinkId));

        assertTrue(exception.getMessage().contains("Chat with ID " + nonExistingChatId + " not found."));
        verify(chatRepository).existsById(nonExistingChatId);
        verify(chatLinkRepository, never()).save(any());
    }

    @Test
    void addLinkToChat_AddsLink_IfChatExists() {
        ChatLink chatLink = new ChatLink();
        chatLink.setChatId(testChatId);
        chatLink.setLinkId(testLinkId);
        chatLink.setSharedAt(LocalDateTime.now());

        when(chatLinkRepository.findByChatId(testChatId)).thenReturn(List.of(chatLink));

        chatLinkService.addLinkToChat(testChatId, testLinkId);
        Collection<ChatLinkDTO> links = chatLinkService.findAllLinksForChat(testChatId);

        assertEquals(1, links.size());
        assertTrue(links.stream().anyMatch(link -> link.getLinkId() == testLinkId));
        verify(chatRepository).existsById(testChatId);
        verify(chatLinkRepository).save(any());
    }

    @Test
    void findAllLinksForChat_ReturnsCorrectLinks() {
        ChatLink chatLink = new ChatLink();
        chatLink.setChatId(testChatId);
        chatLink.setLinkId(testLinkId);
        chatLink.setSharedAt(LocalDateTime.now());

        when(chatLinkRepository.findByChatId(testChatId)).thenReturn(List.of(chatLink));

        Collection<ChatLinkDTO> links = chatLinkService.findAllLinksForChat(testChatId);

        assertFalse(links.isEmpty());
        assertEquals(1, links.size());
        verify(chatLinkRepository).findByChatId(testChatId);
    }

    @Test
    void removeLinkFromChat_RemovesLink() {
        doNothing().when(chatLinkRepository).deleteByChatIdAndLinkId(testChatId, testLinkId);

        chatLinkService.removeLinkFromChat(testChatId, testLinkId);

        verify(chatLinkRepository).deleteByChatIdAndLinkId(testChatId, testLinkId);
    }
}
