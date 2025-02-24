package backend.academy.scrapper.repository.repository;

import backend.academy.scrapper.dto.ChatDTO;
import java.util.Optional;

public interface InMemoryChatRepository {
    void save(ChatDTO chat);

    Optional<ChatDTO> findById(long chatId);

    void delete(long chatId);
}
