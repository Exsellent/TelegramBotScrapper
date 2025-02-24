package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.ChatDTO;
import backend.academy.scrapper.repository.repository.InMemoryChatRepository;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class ChatServiceImpl implements ChatService {
    private final InMemoryChatRepository chatRepository;

    @Autowired
    public ChatServiceImpl(InMemoryChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Override
    public void register(long chatId) {
        chatRepository.save(new ChatDTO(chatId, LocalDateTime.now()));
    }

    @Override
    public void unregister(long chatId) {
        chatRepository.delete(chatId);
    }
}
