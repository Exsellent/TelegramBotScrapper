package backend.academy.scrapper.repository.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.scrapper.domain.Link;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryLinkRepositoryImplTest {

    private InMemoryLinkRepositoryImpl linkRepository;

    @BeforeEach
    void setUp() {
        linkRepository = new InMemoryLinkRepositoryImpl();
    }

    @Test
    void testSaveAndFindById() {
        // Создаем ссылку с помощью конструктора по умолчанию и сеттеров
        Link link = new Link();
        link.setLinkId(null);
        link.setUrl("https://github.com/test/repo");
        link.setLastCheckTime(LocalDateTime.now());

        Link savedLink = linkRepository.save(link);

        // Проверяем, что ссылка сохранилась и ей присвоился ID
        assertNotNull(savedLink.getLinkId());
        Optional<Link> foundLink = linkRepository.findById(savedLink.getLinkId());
        assertTrue(foundLink.isPresent());
        assertEquals("https://github.com/test/repo", foundLink.get().getUrl());
    }

    @Test
    void testFindByUrl() {
        Link link = new Link();
        link.setUrl("https://github.com/test/repo");
        link.setLastCheckTime(LocalDateTime.now());
        linkRepository.save(link);

        Optional<Link> foundLink = linkRepository.findByUrl("https://github.com/test/repo");
        assertTrue(foundLink.isPresent());
        assertEquals("https://github.com/test/repo", foundLink.get().getUrl());
    }

    @Test
    void testDelete() {
        Link link = new Link();
        link.setUrl("https://github.com/test/repo");
        link.setLastCheckTime(LocalDateTime.now());
        Link savedLink = linkRepository.save(link);

        linkRepository.delete(savedLink);

        Optional<Link> foundLink = linkRepository.findById(savedLink.getLinkId());
        assertFalse(foundLink.isPresent());
    }

    @Test
    void testFindByLastCheckTimeBefore() {
        Link oldLink = new Link();
        oldLink.setUrl("https://github.com/old");
        oldLink.setLastCheckTime(LocalDateTime.now().minusDays(2));

        Link newLink = new Link();
        newLink.setUrl("https://github.com/new");
        newLink.setLastCheckTime(LocalDateTime.now());

        linkRepository.save(oldLink);
        linkRepository.save(newLink);

        var results = linkRepository.findByLastCheckTimeBeforeOrLastCheckTimeIsNull(
                LocalDateTime.now().minusDays(1));
        assertEquals(1, results.size());
        assertEquals("https://github.com/old", results.get(0).getUrl());
    }

    @Test
    void testDuplicateLinkNotAllowed() {
        Link link1 = new Link();
        link1.setUrl("https://github.com/test/repo");
        link1.setLastCheckTime(LocalDateTime.now());

        Link savedLink1 = linkRepository.save(link1);

        // Попытка сохранить ту же ссылку снова
        Link link2 = new Link();
        link2.setUrl("https://github.com/test/repo");
        link2.setLastCheckTime(LocalDateTime.now());

        Link savedLink2 = linkRepository.save(link2);

        // cмотрим, что в репозитории осталась только одна ссылка
        long count = linkRepository.findAll().stream()
                .filter(l -> l.getUrl().equals("https://github.com/test/repo"))
                .count();

        assertEquals(1, count, "Дубликаты ссылок не должны сохраняться!");
    }
}
