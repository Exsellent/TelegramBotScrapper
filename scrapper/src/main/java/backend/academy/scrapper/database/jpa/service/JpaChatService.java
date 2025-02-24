package backend.academy.scrapper.database.jpa.service;

import backend.academy.scrapper.domain.Chat;
import backend.academy.scrapper.exception.ChatAlreadyRegisteredException;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.repository.repository.ChatRepository;
import backend.academy.scrapper.service.ChatService;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

public class JpaChatService implements ChatService {

    private final ChatRepository chatRepository;

    public JpaChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Override
    @Transactional
    public void register(long chatId) {
        if (chatRepository.existsById(chatId)) {
            throw new ChatAlreadyRegisteredException("Chat with id " + chatId + " already exists.");
        }
        Chat chat = new Chat();
        chat.setChatId(chatId);
        chat.setCreatedAt(LocalDateTime.now());
        chatRepository.save(chat);
    }

    @Override
    @Transactional
    public void unregister(long chatId) {
        if (!chatRepository.existsById(chatId)) {
            throw new ChatNotFoundException("Chat with Id " + chatId + " not found.");
        }
        chatRepository.deleteById(chatId);
    }
}
