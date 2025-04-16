package backend.academy.scrapper.database.dao;

import backend.academy.scrapper.dao.LinkDao;
import backend.academy.scrapper.dto.LinkDTO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "jdbc")
public class JdbcLinkDao implements LinkDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLinkDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<LinkDTO> LINK_MAPPER = new RowMapper<>() {
        @Override
        public LinkDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp createdAtTs = rs.getTimestamp("created_at");
            Timestamp lastCheckTimeTs = rs.getTimestamp("last_check_time");
            Timestamp lastUpdateTimeTs = rs.getTimestamp("last_update_time");

            return LinkDTO.builder()
                    .linkId(rs.getLong("link_id"))
                    .url(rs.getString("url"))
                    .description(rs.getString("description"))
                    .createdAt(createdAtTs != null ? createdAtTs.toLocalDateTime() : null)
                    .lastCheckTime(lastCheckTimeTs != null ? lastCheckTimeTs.toLocalDateTime() : null)
                    .lastUpdateTime(lastUpdateTimeTs != null ? lastUpdateTimeTs.toLocalDateTime() : null)
                    .tags(Collections.emptyList())
                    .build();
        }
    };

    @Override
    public Long add(LinkDTO linkDTO) {
        String sql = "INSERT INTO link (url, description, created_at, last_check_time, last_update_time) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING link_id";
        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                linkDTO.getUrl(),
                linkDTO.getDescription(),
                linkDTO.getCreatedAt(),
                linkDTO.getLastCheckTime(),
                linkDTO.getLastUpdateTime());
    }

    @Override
    public void remove(String url) {
        String sql = "DELETE FROM link WHERE url = ?";
        jdbcTemplate.update(sql, url);
    }

    @Override
    public Collection<LinkDTO> findAll() {
        String sql = "SELECT link_id, url, description, created_at, last_check_time, last_update_time FROM link";
        return jdbcTemplate.query(sql, LINK_MAPPER);
    }

    @Override
    public void update(LinkDTO linkDTO) {
        String sql = "UPDATE link SET url = ?, description = ?, last_check_time = ?, last_update_time = ? "
                + "WHERE link_id = ?";
        jdbcTemplate.update(
                sql,
                linkDTO.getUrl(),
                linkDTO.getDescription(),
                linkDTO.getLastCheckTime(),
                linkDTO.getLastUpdateTime(),
                linkDTO.getLinkId());
    }

    @Override
    public LinkDTO findById(Long linkId) {
        String sql = "SELECT link_id, url, description, created_at, last_check_time, last_update_time "
                + "FROM link WHERE link_id = ?";
        return jdbcTemplate.query(sql, LINK_MAPPER, linkId).stream().findFirst().orElse(null);
    }

    @Override
    public LinkDTO findByUrl(String url) {
        String sql = "SELECT link_id, url, description, created_at, last_check_time, last_update_time "
                + "FROM link WHERE url = ?";
        return jdbcTemplate.query(sql, LINK_MAPPER, url).stream().findFirst().orElse(null);
    }

    @Override
    public Collection<LinkDTO> findLinksNotCheckedSince(LocalDateTime sinceTime, int offset, int limit) {
        String sql = "SELECT link_id, url, description, created_at, last_check_time, last_update_time "
                + "FROM link WHERE last_check_time IS NULL OR last_check_time < ? "
                + "LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, LINK_MAPPER, sinceTime, limit, offset);
    }

    @Override
    public boolean existsByUrl(String url) {
        String sql = "SELECT COUNT(*) FROM link WHERE url = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, url);
        return count > 0;
    }

    @Override
    public void addTagToLink(Long linkId, String tagName) {}

    @Override
    public void removeTagFromLink(Long linkId, String tagName) {}

    @Override
    public Collection<LinkDTO> findLinksByTag(String tagName) {
        return Collections.emptyList();
    }
}
