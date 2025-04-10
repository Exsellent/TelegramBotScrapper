package backend.academy.scrapper.repository.repository;

import backend.academy.scrapper.domain.ChatLink;
import backend.academy.scrapper.domain.ChatLinkId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatLinkRepository extends JpaRepository<ChatLink, ChatLinkId> {

    List<ChatLink> findByChatId(@Param("chatId") long chatId);

    List<ChatLink> findByLinkId(@Param("linkId") long linkId);

    boolean existsByLinkId(@Param("linkId") long linkId);

    @Modifying
    @Query("DELETE FROM ChatLink cl WHERE cl.chatId = :chatId AND cl.linkId = :linkId")
    void deleteByChatIdAndLinkId(@Param("chatId") long chatId, @Param("linkId") long linkId);
}
