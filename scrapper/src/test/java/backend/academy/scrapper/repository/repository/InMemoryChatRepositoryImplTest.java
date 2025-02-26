package backend.academy.scrapper.repository.repository;

import backend.academy.scrapper.dto.ChatDTO;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryChatRepositoryImplTest {

    private InMemoryChatRepositoryImpl chatRepository;

    @BeforeEach
    void setUp() {
        chatRepository = new InMemoryChatRepositoryImpl();
    }

    @Test
    void testSaveAndFindById() {
        // Передаем правильные параметры: chatId и createdAt
        ChatDTO chat = new ChatDTO(123L, LocalDateTime.now());
        chatRepository.save(chat);

        Optional<ChatDTO> foundChat = chatRepository.findById(123L);
        assertTrue(foundChat.isPresent());
        assertEquals(123L, foundChat.get().getChatId());
    }

    @Test
    void testDelete() {
        ChatDTO chat = new ChatDTO(123L, LocalDateTime.now());
        chatRepository.save(chat);

        chatRepository.delete(123L);

        Optional<ChatDTO> foundChat = chatRepository.findById(123L);
        assertFalse(foundChat.isPresent());
    }
}
