package backend.academy.scrapper.repository.repository;

import backend.academy.scrapper.domain.Link;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findByUrl(String url);

    boolean existsByUrl(String url);

    @Query("SELECT l FROM Link l WHERE l.lastCheckTime IS NULL OR l.lastCheckTime < :sinceTime")
    List<Link> findByLastCheckTimeBeforeOrLastCheckTimeIsNull(
            @Param("sinceTime") LocalDateTime sinceTime, Pageable pageable);

    List<Link> findByTags_Name(String tagName); // метод для поиска по тегам
}
