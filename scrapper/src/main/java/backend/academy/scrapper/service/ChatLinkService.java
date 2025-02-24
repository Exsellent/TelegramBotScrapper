package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.ChatLinkDTO;
import java.util.Collection;

public interface ChatLinkService {

    void addLinkToChat(long chatId, long linkId);

    void removeLinkFromChat(long chatId, long linkId);

    Collection<ChatLinkDTO> findAllLinksForChat(long chatId);

    Collection<ChatLinkDTO> findAllChatsForLink(long linkId);

    boolean existsChatsForLink(long linkId);
}
