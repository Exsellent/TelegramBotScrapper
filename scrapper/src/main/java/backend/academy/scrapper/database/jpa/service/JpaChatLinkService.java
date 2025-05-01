package backend.academy.scrapper.database.jpa.service;

import backend.academy.scrapper.domain.ChatLink;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.repository.repository.ChatLinkRepository;
import backend.academy.scrapper.repository.repository.ChatRepository;
import backend.academy.scrapper.service.ChatLinkService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public JpaChatLinkService(
            ChatLinkRepository chatLinkRepository, ChatRepository chatRepository, ObjectMapper objectMapper) {
        this.chatLinkRepository = chatLinkRepository;
        this.chatRepository = chatRepository;
        this.objectMapper = objectMapper;
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
        try {
            chatLink.setFilters(filters != null ? objectMapper.writeValueAsString(filters) : null);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize filters to JSON", e);
            throw new RuntimeException("Error serializing filters", e);
        }
        chatLinkRepository.save(chatLink);
        LOGGER.info("Link {} added to chat {}", linkId, chatId);
    }

    @Override
    public void addLinkToChat(long chatId, long linkId) throws ChatNotFoundException {
        addLinkToChat(chatId, linkId, null);
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

    @Override
    public Map<String, String> getFiltersForLink(long linkId) {
        LOGGER.info("Fetching filters for link {}", linkId);
        return chatLinkRepository.findByLinkId(linkId).stream()
                .findFirst()
                .map(chatLink -> {
                    try {
                        return chatLink.getFilters() != null
                                ? objectMapper.readValue(chatLink.getFilters(), Map.class)
                                : Map.of();
                    } catch (Exception e) {
                        LOGGER.error("Failed to deserialize filters from JSON", e);
                        return Map.of();
                    }
                })
                .orElse(Map.of());
    }

    private ChatLinkDTO mapToDto(ChatLink chatLink) {
        try {
            Map<String, String> filters =
                    chatLink.getFilters() != null ? objectMapper.readValue(chatLink.getFilters(), Map.class) : null;
            return new ChatLinkDTO(chatLink.getChatId(), chatLink.getLinkId(), filters, chatLink.getSharedAt());
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize filters from JSON", e);
            return new ChatLinkDTO(chatLink.getChatId(), chatLink.getLinkId(), null, chatLink.getSharedAt());
        }
    }
}
