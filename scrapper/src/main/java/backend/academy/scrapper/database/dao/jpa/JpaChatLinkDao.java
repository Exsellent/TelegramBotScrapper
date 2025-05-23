package backend.academy.scrapper.database.dao.jpa;

import backend.academy.scrapper.dao.ChatLinkDao;
import backend.academy.scrapper.domain.ChatLink;
import backend.academy.scrapper.repository.repository.ChatLinkRepository;
import java.util.Collection;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "jpa")
public class JpaChatLinkDao implements ChatLinkDao {

    private final ChatLinkRepository chatLinkRepository;

    @Override
    public void add(ChatLink chatLink) {
        chatLinkRepository.save(chatLink);
    }

    @Override
    public void removeByChatIdAndLinkId(long chatId, long linkId) {
        chatLinkRepository.deleteByChatIdAndLinkId(chatId, linkId);
    }

    @Override
    public Collection<ChatLink> findByChatId(long chatId) {
        return chatLinkRepository.findByChatId(chatId);
    }

    @Override
    public Collection<ChatLink> findByLinkId(long linkId) {
        return chatLinkRepository.findByLinkId(linkId);
    }

    @Override
    public boolean existsByLinkId(long linkId) {
        return chatLinkRepository.existsByLinkId(linkId);
    }

    @Override
    public Collection<ChatLink> findAll() {
        return chatLinkRepository.findAll();
    }
}
