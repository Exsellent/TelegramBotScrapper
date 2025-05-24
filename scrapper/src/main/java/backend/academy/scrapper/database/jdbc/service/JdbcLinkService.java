package backend.academy.scrapper.database.jdbc.service;

import backend.academy.scrapper.dao.LinkDao;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.exception.LinkAlreadyAddedException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.service.GitHubService;
import backend.academy.scrapper.service.LinkService;
import backend.academy.scrapper.service.StackOverflowService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "jdbc")
@Primary
public class JdbcLinkService implements LinkService {

    private final LinkDao linkDao;
    private final MeterRegistry meterRegistry;
    private final GitHubService gitHubService; // Добавлено
    private final StackOverflowService stackOverflowService; // Добавлено

    private static final String NOT_FOUND = " not found.";

    @Override
    public LinkDTO add(String url, String description) throws LinkAlreadyAddedException {
        if (linkDao.existsByUrl(url)) {
            throw new LinkAlreadyAddedException(url + " already exists.");
        }
        LinkDTO link = LinkDTO.builder()
                .linkId(null)
                .url(url)
                .description(description)
                .createdAt(LocalDateTime.now())
                .lastCheckTime(null)
                .lastUpdateTime(null)
                .tags(new ArrayList<>())
                .build();
        link.setLinkId(linkDao.add(link));
        registerActiveLinksMetrics();
        return link;
    }

    @Override
    public void remove(String url) {
        if (!linkDao.existsByUrl(url)) {
            throw new LinkNotFoundException(url + NOT_FOUND);
        }
        linkDao.remove(url);
        registerActiveLinksMetrics();
    }

    @Override
    public Collection<LinkDTO> listAll() {
        return linkDao.findAll();
    }

    @Override
    public void update(LinkDTO link) {
        if (!linkDao.existsByUrl(link.getUrl())) {
            throw new LinkNotFoundException(link.getUrl() + NOT_FOUND);
        }
        linkDao.update(link);
    }

    @Override
    public LinkDTO findById(Long linkId) {
        LinkDTO foundDTO = linkDao.findById(linkId);
        if (foundDTO == null) {
            throw new LinkNotFoundException(linkId + NOT_FOUND);
        }
        return foundDTO;
    }

    @Override
    public LinkDTO findByUrl(String linkUrl) {
        LinkDTO foundDTO = linkDao.findByUrl(linkUrl);
        if (foundDTO == null) {
            throw new LinkNotFoundException(linkUrl + NOT_FOUND);
        }
        return foundDTO;
    }

    @Override
    public Collection<LinkDTO> findLinksToCheck(LocalDateTime sinceTime, int offset, int limit) {
        return linkDao.findLinksNotCheckedSince(sinceTime, offset, limit);
    }

    @Override
    public void addTagToLink(Long linkId, String tagName) {
        if (linkDao.findById(linkId) == null) {
            throw new LinkNotFoundException(linkId + NOT_FOUND);
        }
        linkDao.addTagToLink(linkId, tagName);
    }

    @Override
    public void removeTagFromLink(Long linkId, String tagName) {
        if (linkDao.findById(linkId) == null) {
            throw new LinkNotFoundException(linkId + NOT_FOUND);
        }
        linkDao.removeTagFromLink(linkId, tagName);
    }

    @Override
    public Collection<LinkDTO> findLinksByTag(String tagName) {
        return linkDao.findLinksByTag(tagName);
    }

    @Override
    public void registerActiveLinksMetrics() {
        Collection<LinkDTO> links = linkDao.findAll();
        long githubCount = links.stream()
                .filter(link -> link.getUrl().contains("github.com"))
                .count();
        meterRegistry.gauge(
                "scrapper.links.active", List.of(io.micrometer.core.instrument.Tag.of("type", "github")), githubCount);

        long stackoverflowCount = links.stream()
                .filter(link -> link.getUrl().contains("stackoverflow.com"))
                .count();
        meterRegistry.gauge(
                "scrapper.links.active",
                List.of(io.micrometer.core.instrument.Tag.of("type", "stackoverflow")),
                stackoverflowCount);
    }

    @Override
    public void checkLinkUpdates() {
        LocalDateTime sinceTime = LocalDateTime.now().minusMinutes(5);
        Collection<LinkDTO> links = findLinksToCheck(sinceTime, 0, 100);
        for (LinkDTO link : links) {
            String url = link.getUrl();
            if (url.contains("github.com")) {
                gitHubService.fetchUpdates(url);
            } else if (url.contains("stackoverflow.com")) {
                stackOverflowService.fetchUpdates(url);
            }
        }
    }
}
