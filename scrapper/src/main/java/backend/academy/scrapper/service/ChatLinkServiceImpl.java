package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.ChatLinkDTO;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChatLinkServiceImpl implements ChatLinkService {
    // Используем ConcurrentHashMap для потокобезопасности
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, ChatLinkDTO>> chatToLinks = new ConcurrentHashMap<>();

    @Override
    public void addLinkToChat(long chatId, long linkId) {
        chatToLinks
                .computeIfAbsent(chatId, k -> new ConcurrentHashMap<>())
                .put(linkId, new ChatLinkDTO(chatId, linkId));
    }

    @Override
    public void removeLinkFromChat(long chatId, long linkId) {
        if (chatToLinks.containsKey(chatId)) {
            chatToLinks.get(chatId).remove(linkId);
            // Если для чата больше нет ссылок, удаляем запись о чате
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
    public boolean existsChatsForLink(long linkId) {
        return chatToLinks.values().stream().anyMatch(links -> links.containsKey(linkId));
    }
}
