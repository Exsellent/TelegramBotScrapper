package backend.academy.scrapper.repository.repository;

import backend.academy.scrapper.domain.Link;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LinkRepository {
    Link save(Link link);

    void delete(Link link);

    Optional<Link> findByUrl(String url);

    List<Link> findByLastCheckTimeBeforeOrLastCheckTimeIsNull(LocalDateTime dateTime);

    boolean existsByUrl(String url);

    Optional<Link> findById(Long id);

    List<Link> findAll();
}
