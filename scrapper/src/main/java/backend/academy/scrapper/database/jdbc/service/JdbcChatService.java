package backend.academy.scrapper.database.jdbc.service;

import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.dto.ChatDTO;
import backend.academy.scrapper.exception.ChatAlreadyRegisteredException;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.service.ChatService;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class JdbcChatService implements ChatService {

    private final ChatDao chatDao;

    @Override
    public void register(long chatId) {
        if (chatDao.existsById(chatId)) {
            throw new ChatAlreadyRegisteredException("Chat with id " + chatId + " already exists.");
        }
        chatDao.add(new ChatDTO(chatId, LocalDateTime.now()));
    }

    @Override
    public void unregister(long chatId) {
        if (!chatDao.existsById(chatId)) {
            throw new ChatNotFoundException("Chat with Id " + chatId + " not found.");
        }
        chatDao.remove(chatId);
    }
}
