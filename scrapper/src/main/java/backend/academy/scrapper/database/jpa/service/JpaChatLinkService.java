package backend.academy.scrapper.database.jpa.service;

import backend.academy.scrapper.domain.ChatLink;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.repository.repository.ChatLinkRepository;
import backend.academy.scrapper.repository.repository.ChatRepository;
import backend.academy.scrapper.service.ChatLinkService;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service("jpaChatLinkService")
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "jpa")
public class JpaChatLinkService implements ChatLinkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaChatLinkService.class);
    private final ChatLinkRepository chatLinkRepository;
    private final ChatRepository chatRepository;

    public JpaChatLinkService(ChatLinkRepository chatLinkRepository, ChatRepository chatRepository) {
        this.chatLinkRepository = chatLinkRepository;
        this.chatRepository = chatRepository;
    }

    @Override
    public void addLinkToChat(long chatId, long linkId, Map<String, String> filters) throws ChatNotFoundException {
        LOGGER.info("Adding link {} to chat {} with filters {}", linkId, chatId, filters);
        if (!chatRepository.existsById(chatId)) {
            LOGGER.error("Chat with ID {} not found", chatId);
            throw new ChatNotFoundException("Chat with ID " + chatId + " not found.");
        }
        ChatLink chatLink = new ChatLink();
        chatLink.setChatId(chatId);
        chatLink.setLinkId(linkId);
        chatLink.setSharedAt(LocalDateTime.now());
        chatLink.setFilters(filters); // Сохраняем фильтры
        chatLinkRepository.save(chatLink);
        LOGGER.info("Link {} added to chat {}", linkId, chatId);
    }

    @Override
    public void addLinkToChat(long chatId, long linkId) throws ChatNotFoundException {
        addLinkToChat(chatId, linkId, null); // Без фильтров
    }

    @Override
    public void removeLinkFromChat(long chatId, long linkId) {
        LOGGER.info("Removing link {} from chat {}", linkId, chatId);
        chatLinkRepository.deleteByChatIdAndLinkId(chatId, linkId);
        LOGGER.info("Link {} removed from chat {}", linkId, chatId);
    }

    @Override
    public Collection<ChatLinkDTO> findAllLinksForChat(long chatId) {
        LOGGER.info("Finding all links for chat {}", chatId);
        Collection<ChatLinkDTO> links = chatLinkRepository.findByChatId(chatId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        LOGGER.info("Found {} links for chat {}", links.size(), chatId);
        return links;
    }

    @Override
    public Collection<ChatLinkDTO> findAllChatsForLink(long linkId) {
        LOGGER.info("Finding all chats for link {}", linkId);
        Collection<ChatLinkDTO> chats = chatLinkRepository.findByLinkId(linkId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        LOGGER.info("Found {} chats for link {}", chats.size(), linkId);
        return chats;
    }

    @Override
    public boolean existsChatsForLink(long linkId) {
        LOGGER.info("Checking if chats exist for link {}", linkId);
        boolean exists = chatLinkRepository.existsByLinkId(linkId);
        LOGGER.info("Chats exist for link {}: {}", linkId, exists);
        return exists;
    }

    private ChatLinkDTO mapToDto(ChatLink chatLink) {
        return new ChatLinkDTO(
                chatLink.getChatId(), chatLink.getLinkId(), chatLink.getFilters(), chatLink.getSharedAt());
    }

    @Override
    public Map<String, String> getFiltersForLink(long linkId) {
        LOGGER.info("Fetching filters for link {}", linkId);
        return chatLinkRepository.findByLinkId(linkId).stream()
                .findFirst()
                .map(ChatLink::getFilters)
                .orElse(Map.of());
    }
}
