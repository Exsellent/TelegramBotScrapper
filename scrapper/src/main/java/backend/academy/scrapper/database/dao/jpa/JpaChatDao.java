package backend.academy.scrapper.database.dao.jpa;

import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.domain.Chat;
import backend.academy.scrapper.dto.ChatDTO;
import backend.academy.scrapper.repository.repository.ChatRepository;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "jpa")
public class JpaChatDao implements ChatDao {

    private final ChatRepository chatRepository;

    @Override
    public void add(ChatDTO chatDTO) {
        Chat chat = new Chat();
        chat.setChatId(chatDTO.getChatId());
        chat.setCreatedAt(chatDTO.getCreatedAt());
        chatRepository.save(chat);
    }

    @Override
    public boolean existsById(long chatId) {
        return chatRepository.existsById(chatId);
    }

    @Override
    public void remove(long chatId) {
        chatRepository.deleteById(chatId);
    }

    @Override
    public Collection<ChatDTO> findAll() {
        return chatRepository.findAll().stream()
                .map(chat -> new ChatDTO(chat.getChatId(), chat.getCreatedAt()))
                .collect(Collectors.toList());
    }
}
