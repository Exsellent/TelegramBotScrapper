package backend.academy.scrapper.database.jpa.jdbc.dao;

import backend.academy.scrapper.dao.LinkDao;
import backend.academy.scrapper.dto.LinkDTO;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@AllArgsConstructor
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "JDBC") // ✅ Теперь создается только если JDBC
@SuppressWarnings("MultipleStringLiterals")
public class JdbcLinkDao implements LinkDao {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    @SuppressWarnings("MagicNumber")
    public Long add(LinkDTO link) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO link (url, description, created_at) VALUES (?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, link.getUrl());
                    ps.setString(2, link.getDescription());
                    ps.setTimestamp(3, Timestamp.valueOf(link.getCreatedAt()));
                    return ps;
                },
                keyHolder);

        List<Map<String, Object>> keyList = keyHolder.getKeyList();
        if (!keyList.isEmpty()) {
            Map<String, Object> keys = keyList.get(0);
            if (keys.containsKey("link_id")) {
                return ((Number) keys.get("link_id")).longValue();
            }
        }
        return null;
    }

    @Transactional
    @Override
    public void remove(Long linkId) {
        jdbcTemplate.update("DELETE FROM link WHERE link_id = ?", linkId);
    }

    @Transactional
    @Override
    public void remove(String url) {
        jdbcTemplate.update("DELETE FROM link WHERE url = ?", url);
    }

    @Override
    public List<LinkDTO> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM link",
                (rs, rowNum) -> new LinkDTO(
                        rs.getLong("link_id"),
                        rs.getString("url"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("last_check_time") != null
                                ? rs.getTimestamp("last_check_time").toLocalDateTime()
                                : null,
                        rs.getTimestamp("last_update_time") != null
                                ? rs.getTimestamp("last_update_time").toLocalDateTime()
                                : null));
    }

    @Transactional
    @Override
    public void update(LinkDTO link) {
        jdbcTemplate.update(
                "UPDATE link SET url = ?, description = ?, last_check_time = ?, last_update_time = ? WHERE link_id = ?",
                link.getUrl(),
                link.getDescription(),
                link.getLastCheckTime() != null ? Timestamp.valueOf(link.getLastCheckTime()) : null,
                link.getLastUpdateTime() != null ? Timestamp.valueOf(link.getLastUpdateTime()) : null,
                link.getLinkId());
    }

    @Override
    public List<LinkDTO> findLinksNotCheckedSince(LocalDateTime dateTime) {
        return jdbcTemplate.query(
                "SELECT * FROM link WHERE last_check_time <= ? OR last_check_time IS NULL",
                new Object[] {Timestamp.valueOf(dateTime)},
                (rs, rowNum) -> new LinkDTO(
                        rs.getLong("link_id"),
                        rs.getString("url"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("last_check_time") != null
                                ? rs.getTimestamp("last_check_time").toLocalDateTime()
                                : null,
                        rs.getTimestamp("last_update_time") != null
                                ? rs.getTimestamp("last_update_time").toLocalDateTime()
                                : null));
    }

    @Override
    public boolean existsByUrl(String url) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM link WHERE url = ?", new Object[] {url}, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public LinkDTO findById(Long linkId) {
        List<LinkDTO> result = jdbcTemplate.query(
                "SELECT * FROM link WHERE link_id = ?",
                new Object[] {linkId},
                (rs, rowNum) -> new LinkDTO(
                        rs.getLong("link_id"),
                        rs.getString("url"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("last_check_time") != null
                                ? rs.getTimestamp("last_check_time").toLocalDateTime()
                                : null,
                        rs.getTimestamp("last_update_time") != null
                                ? rs.getTimestamp("last_update_time").toLocalDateTime()
                                : null));
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public LinkDTO findByUrl(String linkUrl) {
        List<LinkDTO> result = jdbcTemplate.query(
                "SELECT * FROM link WHERE url = ?",
                new Object[] {linkUrl},
                (rs, rowNum) -> new LinkDTO(
                        rs.getLong("link_id"),
                        rs.getString("url"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("last_check_time") != null
                                ? rs.getTimestamp("last_check_time").toLocalDateTime()
                                : null,
                        rs.getTimestamp("last_update_time") != null
                                ? rs.getTimestamp("last_update_time").toLocalDateTime()
                                : null));
        return result.isEmpty() ? null : result.get(0);
    }
}
