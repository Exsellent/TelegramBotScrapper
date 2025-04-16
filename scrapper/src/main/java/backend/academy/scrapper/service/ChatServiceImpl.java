package backend.academy.scrapper.service;

import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.dto.ChatDTO;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatDao chatDao;

    @Autowired
    public ChatServiceImpl(ChatDao chatDao) {
        this.chatDao = chatDao;
    }

    @Override
    public void register(long chatId) {
        if (!chatDao.existsById(chatId)) {
            chatDao.add(new ChatDTO(chatId, LocalDateTime.now()));
        }
    }

    @Override
    public void unregister(long chatId) {
        chatDao.remove(chatId);
    }
}
