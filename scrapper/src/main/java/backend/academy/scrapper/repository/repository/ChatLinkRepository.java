package backend.academy.scrapper.repository.repository;

import backend.academy.scrapper.domain.ChatLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ChatLinkRepository extends JpaRepository<ChatLink, backend.academy.scrapper.domain.ChatLinkId> {

    List<ChatLink> findByLinkId(Long linkId);

    List<ChatLink> findByChatId(Long chatId);

    boolean existsByLinkId(Long linkId);

    @Transactional
    void deleteByChatIdAndLinkId(Long chatId, Long linkId);
}
