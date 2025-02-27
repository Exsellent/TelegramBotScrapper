package backend.academy.scrapper.database.jpa.jdbc.dao;

import backend.academy.scrapper.dao.ChatLinkDao;
import backend.academy.scrapper.dto.ChatLinkDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@AllArgsConstructor
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "JDBC")
@SuppressWarnings("MultipleStringLiterals")
public class JdbcChatLinkDao implements ChatLinkDao {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    @Override
    public void add(ChatLinkDTO chatLink) {
        jdbcTemplate.update(
                "INSERT INTO chat_link (chat_id, link_id, shared_at) VALUES (?, ?, ?)",
                chatLink.getChatId(),
                chatLink.getLinkId(),
                chatLink.getSharedAt());
    }

    @Transactional
    @Override
    public void remove(Long chatId, Long linkId) {
        jdbcTemplate.update("DELETE FROM chat_link WHERE chat_id = ? AND link_id = ?", chatId, linkId);
    }

    @Override
    public List<ChatLinkDTO> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM chat_link",
                (rs, rowNum) -> new ChatLinkDTO(
                        rs.getLong("chat_id"),
                        rs.getLong("link_id"),
                        rs.getTimestamp("shared_at").toLocalDateTime()));
    }

    @Override
    public List<ChatLinkDTO> getChatsForLink(Long linkId) {
        return jdbcTemplate.query(
                "SELECT * FROM chat_link WHERE link_id = ?",
                new Object[] {linkId},
                (rs, rowNum) -> new ChatLinkDTO(
                        rs.getLong("chat_id"),
                        rs.getLong("link_id"),
                        rs.getTimestamp("shared_at").toLocalDateTime()));
    }
}
