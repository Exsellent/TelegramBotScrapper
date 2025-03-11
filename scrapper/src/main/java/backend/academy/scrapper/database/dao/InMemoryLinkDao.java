package backend.academy.scrapper.database.dao;

import backend.academy.scrapper.dao.LinkDao;
import backend.academy.scrapper.dto.LinkDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "IN_MEMORY", matchIfMissing = true)
public class InMemoryLinkDao implements LinkDao {
    private final Map<Long, LinkDTO> links = new ConcurrentHashMap<>();
    private long idCounter = 1;

    @Override
    public Long add(LinkDTO link) {
        long linkId = idCounter++;
        LinkDTO newLink = new LinkDTO(linkId, link.getUrl(), link.getDescription(), LocalDateTime.now(), null, null);
        links.put(linkId, newLink);
        return linkId;
    }

    @Override
    public void remove(Long linkId) {
        links.remove(linkId);
    }

    @Override
    public void remove(String url) {
        links.entrySet().removeIf(entry -> entry.getValue().getUrl().equals(url));
    }

    @Override
    public List<LinkDTO> findAll() {
        return links.values().stream().collect(Collectors.toList());
    }

    @Override
    public void update(LinkDTO link) {
        links.put(link.getLinkId(), link);
    }

    @Override
    public boolean existsByUrl(String url) {
        return links.values().stream().anyMatch(link -> link.getUrl().equals(url));
    }

    @Override
    public LinkDTO findById(Long linkId) {
        return links.get(linkId);
    }

    @Override
    public LinkDTO findByUrl(String linkUrl) {
        return links.values().stream()
                .filter(link -> link.getUrl().equals(linkUrl))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<LinkDTO> findLinksNotCheckedSince(LocalDateTime dateTime) {
        return links.values().stream()
                .filter(link -> link.getLastCheckTime() == null
                        || link.getLastCheckTime().isBefore(dateTime))
                .collect(Collectors.toList());
    }
}
