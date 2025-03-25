package backend.academy.scrapper.database.dao;

import backend.academy.scrapper.dao.ChatLinkDao;
import backend.academy.scrapper.domain.ChatLink;
import java.util.Collection;
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
    public void add(ChatLink chatLink) {
        jdbcTemplate.update(
                "INSERT INTO chat_link (chat_id, link_id, shared_at) VALUES (?, ?, ?)",
                chatLink.getChatId(),
                chatLink.getLinkId(),
                chatLink.getSharedAt());
    }

    @Transactional
    @Override
    public void removeByChatIdAndLinkId(long chatId, long linkId) {
        jdbcTemplate.update("DELETE FROM chat_link WHERE chat_id = ? AND link_id = ?", chatId, linkId);
    }

    @Override
    public Collection<ChatLink> findByChatId(long chatId) {
        return jdbcTemplate.query("SELECT * FROM chat_link WHERE chat_id = ?", new Object[] {chatId}, (rs, rowNum) -> {
            ChatLink chatLink = new ChatLink();
            chatLink.setChatId(rs.getLong("chat_id"));
            chatLink.setLinkId(rs.getLong("link_id"));
            chatLink.setSharedAt(rs.getTimestamp("shared_at").toLocalDateTime());
            return chatLink;
        });
    }

    @Override
    public Collection<ChatLink> findByLinkId(long linkId) {
        return jdbcTemplate.query("SELECT * FROM chat_link WHERE link_id = ?", new Object[] {linkId}, (rs, rowNum) -> {
            ChatLink chatLink = new ChatLink();
            chatLink.setChatId(rs.getLong("chat_id"));
            chatLink.setLinkId(rs.getLong("link_id"));
            chatLink.setSharedAt(rs.getTimestamp("shared_at").toLocalDateTime());
            return chatLink;
        });
    }

    @Override
    public boolean existsByLinkId(long linkId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_link WHERE link_id = ?", new Object[] {linkId}, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public Collection<ChatLink> findAll() {
        return jdbcTemplate.query("SELECT * FROM chat_link", (rs, rowNum) -> {
            ChatLink chatLink = new ChatLink();
            chatLink.setChatId(rs.getLong("chat_id"));
            chatLink.setLinkId(rs.getLong("link_id"));
            chatLink.setSharedAt(rs.getTimestamp("shared_at").toLocalDateTime());
            return chatLink;
        });
    }
}
