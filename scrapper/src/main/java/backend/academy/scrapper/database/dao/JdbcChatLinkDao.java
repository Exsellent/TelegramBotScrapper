package backend.academy.scrapper.database.dao;

import backend.academy.scrapper.dao.ChatLinkDao;
import backend.academy.scrapper.domain.ChatLink;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
@AllArgsConstructor
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "JDBC")
public class JdbcChatLinkDao implements ChatLinkDao {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private ChatLink mapRow(ResultSet rs, int rowNum) throws SQLException {
        ChatLink chatLink = new ChatLink();
        chatLink.setChatId(rs.getLong("chat_id"));
        chatLink.setLinkId(rs.getLong("link_id"));
        chatLink.setSharedAt(rs.getObject("shared_at", java.time.LocalDateTime.class));
        String filtersJson = rs.getString("filters");
        if (filtersJson != null) {
            try {
                chatLink.setFilters(objectMapper.readValue(filtersJson, Map.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error deserializing filters from JSON", e);
            }
        }
        return chatLink;
    }

    @Transactional
    @Override
    public void add(ChatLink chatLink) {
        String sql = "INSERT INTO chat_link (chat_id, link_id, shared_at, filters) VALUES (?, ?, ?, ?::json)";
        try {
            String filtersJson = chatLink.getFilters() != null ? objectMapper.writeValueAsString(chatLink.getFilters()) : null;
            jdbcTemplate.update(sql, chatLink.getChatId(), chatLink.getLinkId(), chatLink.getSharedAt(), filtersJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing filters to JSON", e);
        }
    }

    @Override
    public List<ChatLink> findByChatId(long chatId) {
        String sql = "SELECT * FROM chat_link WHERE chat_id = ?";
        return jdbcTemplate.query(sql, this::mapRow, chatId);
    }

    @Transactional
    @Override
    public void removeByChatIdAndLinkId(long chatId, long linkId) {
        jdbcTemplate.update(
            "DELETE FROM chat_link WHERE chat_id = ? AND link_id = ?",
            chatId, linkId
        );
    }

    @Override
    public Collection<ChatLink> findByLinkId(long linkId) {
        String sql = "SELECT * FROM chat_link WHERE link_id = ?";
        return jdbcTemplate.query(sql, this::mapRow, linkId);
    }

    @Override
    public boolean existsByLinkId(long linkId) {
        int count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_link WHERE link_id = ?",
            Integer.class, linkId
        );
        return count > 0;
    }

    @Override
    public Collection<ChatLink> findAll() {
        String sql = "SELECT * FROM chat_link";
        return jdbcTemplate.query(sql, this::mapRow);
    }
}
