package backend.academy.scrapper.dao;

import backend.academy.scrapper.domain.ChatLink;
import java.util.Collection;

public interface ChatLinkDao {
    void add(ChatLink chatLink);

    void removeByChatIdAndLinkId(long chatId, long linkId);

    Collection<ChatLink> findByChatId(long chatId);

    Collection<ChatLink> findByLinkId(long linkId);

    boolean existsByLinkId(long linkId);

    Collection<ChatLink> findAll();
}
