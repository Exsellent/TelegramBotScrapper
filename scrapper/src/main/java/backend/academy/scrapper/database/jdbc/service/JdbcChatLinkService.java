package backend.academy.scrapper.database.jdbc.service;

import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.dao.ChatLinkDao;
import backend.academy.scrapper.domain.ChatLink;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.service.ChatLinkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "jdbc")
@Primary
public class JdbcChatLinkService implements ChatLinkService {

    private final ChatLinkDao chatLinkDao;
    private final ChatDao chatDao;
    private final ObjectMapper objectMapper;

    @Override
    public void addLinkToChat(long chatId, long linkId, Map<String, String> filters) throws ChatNotFoundException {
        if (!chatDao.existsById(chatId)) {
            throw new ChatNotFoundException("Chat with ID " + chatId + " not found.");
        }
        ChatLink chatLink = new ChatLink();
        chatLink.setChatId(chatId);
        chatLink.setLinkId(linkId);
        chatLink.setSharedAt(LocalDateTime.now());
        try {
            chatLink.setFilters(filters != null ? objectMapper.writeValueAsString(filters) : null);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing filters to JSON", e);
        }
        chatLinkDao.add(chatLink);
    }

    @Override
    public void addLinkToChat(long chatId, long linkId) throws ChatNotFoundException {
        addLinkToChat(chatId, linkId, null); // Без фильтров
    }

    @Override
    public void removeLinkFromChat(long chatId, long linkId) {
        chatLinkDao.removeByChatIdAndLinkId(chatId, linkId);
    }

    @Override
    public Collection<ChatLinkDTO> findAllLinksForChat(long chatId) {
        return chatLinkDao.findByChatId(chatId).stream()
                .map(this::toChatLinkDTO)
                .toList();
    }

    @Override
    public Collection<ChatLinkDTO> findAllChatsForLink(long linkId) {
        return chatLinkDao.findByLinkId(linkId).stream()
                .map(this::toChatLinkDTO)
                .toList();
    }

    @Override
    public boolean existsChatsForLink(long linkId) {
        return chatLinkDao.existsByLinkId(linkId);
    }

    private ChatLinkDTO toChatLinkDTO(ChatLink chatLink) {
        try {
            Map<String, String> filters =
                    chatLink.getFilters() != null ? objectMapper.readValue(chatLink.getFilters(), Map.class) : null;
            return new ChatLinkDTO(chatLink.getChatId(), chatLink.getLinkId(), filters, chatLink.getSharedAt());
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing filters from JSON", e);
        }
    }

    @Override
    public Map<String, String> getFiltersForLink(long linkId) {
        return chatLinkDao.findByLinkId(linkId).stream()
                .findFirst()
                .map(chatLink -> {
                    try {
                        return chatLink.getFilters() != null
                                ? objectMapper.readValue(chatLink.getFilters(), Map.class)
                                : Map.of();
                    } catch (Exception e) {
                        throw new RuntimeException("Error deserializing filters from JSON", e);
                    }
                })
                .orElse(Map.of());
    }
}
