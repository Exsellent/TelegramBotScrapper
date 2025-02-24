package backend.academy.scrapper.database.jpa.jdbc.dao;

import backend.academy.scrapper.dao.ChatLinkDao;
import backend.academy.scrapper.dto.ChatLinkDTO;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "IN_MEMORY", matchIfMissing = true)
public class InMemoryChatLinkDao implements ChatLinkDao {
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, ChatLinkDTO>>
        chatLinks = new ConcurrentHashMap<>();

    @Override
    public void add(ChatLinkDTO chatLink) {
        chatLinks
                .computeIfAbsent(chatLink.getChatId(), k -> new ConcurrentHashMap<>())
                .put(chatLink.getLinkId(), chatLink);
    }

    @Override
    public void remove(Long chatId, Long linkId) {
        if (chatLinks.containsKey(chatId)) {
            chatLinks.get(chatId).remove(linkId);
            if (chatLinks.get(chatId).isEmpty()) {
                chatLinks.remove(chatId);
            }
        }
    }

    @Override
    public List<ChatLinkDTO> findAll() {
        return chatLinks.values().stream().flatMap(map -> map.values()
            .stream()).collect(Collectors.toList());
    }

    @Override
    public List<ChatLinkDTO> getChatsForLink(Long linkId) {
        return chatLinks.values().stream()
                .flatMap(map -> map.values().stream())
                .filter(chatLink -> chatLink.getLinkId().equals(linkId))
                .collect(Collectors.toList());
    }
}
