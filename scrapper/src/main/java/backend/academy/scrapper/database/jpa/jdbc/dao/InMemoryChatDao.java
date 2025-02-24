package backend.academy.scrapper.database.jpa.jdbc.dao;

import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.dto.ChatDTO;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "IN_MEMORY", matchIfMissing = true)
public class InMemoryChatDao implements ChatDao {
    private final ConcurrentHashMap<Long, ChatDTO> chats = new ConcurrentHashMap<>();

    @Override
    public void add(ChatDTO chat) {
        chats.put(chat.getChatId(), chat);
    }

    @Override
    public void remove(Long chatId) {
        chats.remove(chatId);
    }

    @Override
    public List<ChatDTO> findAll() {
        return chats.values().stream().collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Long chatId) {
        return chats.containsKey(chatId);
    }
}
