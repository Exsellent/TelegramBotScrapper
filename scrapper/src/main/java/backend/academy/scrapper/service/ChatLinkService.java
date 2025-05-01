package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.ChatLinkDTO;
import java.util.Collection;
import java.util.Map;

public interface ChatLinkService {
    void addLinkToChat(long chatId, long linkId, Map<String, String> filters);

    void addLinkToChat(long chatId, long linkId);

    void removeLinkFromChat(long chatId, long linkId);

    Collection<ChatLinkDTO> findAllLinksForChat(long chatId);

    Collection<ChatLinkDTO> findAllChatsForLink(long linkId);

    Map<String, String> getFiltersForLink(long linkId);

    boolean existsChatsForLink(long linkId);
}
