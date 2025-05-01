package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.ChatLinkDTO;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChatLinkServiceImpl implements ChatLinkService {
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, ChatLinkDTO>> chatToLinks = new ConcurrentHashMap<>();

    @Override
    public void addLinkToChat(long chatId, long linkId, Map<String, String> filters) {
        chatToLinks
                .computeIfAbsent(chatId, k -> new ConcurrentHashMap<>())
                .put(linkId, new ChatLinkDTO(chatId, linkId, filters, LocalDateTime.now()));
    }

    @Override
    public void addLinkToChat(long chatId, long linkId) {
        addLinkToChat(chatId, linkId, null); // Без фильтров
    }

    @Override
    public void removeLinkFromChat(long chatId, long linkId) {
        if (chatToLinks.containsKey(chatId)) {
            chatToLinks.get(chatId).remove(linkId);
            if (chatToLinks.get(chatId).isEmpty()) {
                chatToLinks.remove(chatId);
            }
        }
    }

    @Override
    public Collection<ChatLinkDTO> findAllLinksForChat(long chatId) {
        return chatToLinks.getOrDefault(chatId, new ConcurrentHashMap<>()).values();
    }

    @Override
    public Collection<ChatLinkDTO> findAllChatsForLink(long linkId) {
        return chatToLinks.values().stream()
                .flatMap(links -> links.values().stream())
                .filter(chatLink -> chatLink.getLinkId() == linkId)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getFiltersForLink(long linkId) {
        Collection<ChatLinkDTO> chatLinks = findAllChatsForLink(linkId);
        if (chatLinks.isEmpty()) {
            return Map.of(); // Нет фильтров, если нет чатов
        }
        // Возвращаем фильтры первого чата (предполагаем, что фильтры одинаковы для всех чатов)
        return chatLinks.iterator().next().getFilters();
    }

    @Override
    public boolean existsChatsForLink(long linkId) {
        return chatToLinks.values().stream().anyMatch(links -> links.containsKey(linkId));
    }
}
