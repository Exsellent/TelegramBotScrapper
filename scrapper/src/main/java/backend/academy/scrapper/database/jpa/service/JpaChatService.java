package backend.academy.scrapper.database.jpa.service;

import backend.academy.scrapper.domain.Chat;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.repository.repository.ChatRepository;
import backend.academy.scrapper.service.ChatService;
import org.springframework.transaction.annotation.Transactional;

public class JpaChatService implements ChatService {

    private final ChatRepository chatRepository;

    public JpaChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Transactional
    @Override
    public void register(long chatId) {
        if (!chatRepository.existsById(chatId)) {
            Chat chat = new Chat();
            chat.setChatId(chatId);
            chatRepository.save(chat);
        }
    }

    @Transactional
    @Override
    public void unregister(long chatId) {
        if (!chatRepository.existsById(chatId)) {
            throw new ChatNotFoundException("Chat with ID " + chatId + " not found.");
        }
        chatRepository.deleteById(chatId);
    }
}
