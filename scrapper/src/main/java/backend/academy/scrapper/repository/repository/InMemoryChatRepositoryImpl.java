package backend.academy.scrapper.repository.repository;

import backend.academy.scrapper.dto.ChatDTO;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryChatRepositoryImpl implements InMemoryChatRepository {
    private final Map<Long, ChatDTO> storage = new ConcurrentHashMap<>();

    @Override
    public void save(ChatDTO chat) {
        storage.put(chat.getChatId(), chat);
    }

    @Override
    public Optional<ChatDTO> findById(long chatId) {
        return Optional.ofNullable(storage.get(chatId));
    }

    @Override
    public void delete(long chatId) {
        storage.remove(chatId);
    }
}
