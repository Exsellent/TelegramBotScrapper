package backend.academy.scrapper.database.dao;

import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.dto.ChatDTO;
import java.sql.Timestamp;
import java.util.Collection;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@AllArgsConstructor
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "jdbc")
public class JdbcChatDao implements ChatDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcChatDao.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void add(ChatDTO chat) {
        LOGGER.info("Adding chat: {}", chat);
        String sql = "INSERT INTO chat (chat_id, created_at) VALUES (?, ?)";
        jdbcTemplate.update(sql, chat.getChatId(), Timestamp.valueOf(chat.getCreatedAt()));
        LOGGER.info("Chat added with ID: {}", chat.getChatId());
    }

    @Override
    @Transactional
    public void remove(long chatId) {
        LOGGER.info("Removing chat with ID: {}", chatId);
        int rowsAffected = jdbcTemplate.update("DELETE FROM chat WHERE chat_id = ?", chatId);
        LOGGER.info("Rows affected: {}", rowsAffected);
    }

    @Override
    public Collection<ChatDTO> findAll() {
        LOGGER.info("Finding all chats");
        Collection<ChatDTO> chats = jdbcTemplate.query(
                "SELECT * FROM chat",
                (rs, rowNum) -> new ChatDTO(
                        rs.getLong("chat_id"), rs.getTimestamp("created_at").toLocalDateTime()));
        LOGGER.info("Found {} chats", chats.size());
        return chats;
    }

    @Override
    public boolean existsById(long chatId) {
        LOGGER.info("Checking if chat exists with ID: {}", chatId);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat WHERE chat_id = ?", new Object[] {chatId}, Integer.class);
        boolean exists = count != null && count > 0;
        LOGGER.info("Chat exists: {}", exists);
        return exists;
    }
}
