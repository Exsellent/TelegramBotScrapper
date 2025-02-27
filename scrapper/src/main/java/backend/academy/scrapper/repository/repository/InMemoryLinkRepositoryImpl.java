package backend.academy.scrapper.repository.repository;

import backend.academy.scrapper.domain.Link;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryLinkRepositoryImpl implements LinkRepository {
    private final Map<Long, Link> storage = new ConcurrentHashMap<>();
    private long idCounter = 1;

    @Override
    public Link save(Link link) {
        Optional<Link> existingLink = findByUrl(link.getUrl());
        if (existingLink.isPresent()) {
            // Если ссылка уже есть, возвращаем её без добавления новой копии
            return existingLink.get();
        }

        if (link.getLinkId() == null) {
            link.setLinkId(idCounter++);
        }
        storage.put(link.getLinkId(), link);
        return link;
    }

    @Override
    public void delete(Link link) {
        storage.remove(link.getLinkId());
    }

    @Override
    public Optional<Link> findByUrl(String url) {
        return storage.values().stream()
                .filter(link -> link.getUrl().equals(url))
                .findFirst();
    }

    @Override
    public List<Link> findByLastCheckTimeBeforeOrLastCheckTimeIsNull(LocalDateTime dateTime) {
        return storage.values().stream()
                .filter(link -> link.getLastCheckTime() == null
                        || link.getLastCheckTime().isBefore(dateTime))
                .toList();
    }

    @Override
    public boolean existsByUrl(String url) {
        return storage.values().stream().anyMatch(link -> link.getUrl().equals(url));
    }

    @Override
    public Optional<Link> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Link> findAll() {
        return new ArrayList<>(storage.values());
    }
}
