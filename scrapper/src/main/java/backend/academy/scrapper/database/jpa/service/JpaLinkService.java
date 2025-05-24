package backend.academy.scrapper.database.jpa.service;

import backend.academy.scrapper.dao.LinkDao;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.exception.LinkAlreadyAddedException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.service.GitHubService;
import backend.academy.scrapper.service.LinkService;
import backend.academy.scrapper.service.StackOverflowService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class JpaLinkService implements LinkService {

    private final LinkDao linkDao;
    private final MeterRegistry meterRegistry;
    private final GitHubService gitHubService; // Добавлено
    private final StackOverflowService stackOverflowService; // Добавлено

    public JpaLinkService(
            LinkDao linkDao,
            MeterRegistry meterRegistry,
            GitHubService gitHubService,
            StackOverflowService stackOverflowService) {
        this.linkDao = linkDao;
        this.meterRegistry = meterRegistry;
        this.gitHubService = gitHubService;
        this.stackOverflowService = stackOverflowService;
    }

    @Override
    public LinkDTO add(String url, String description) {
        if (linkDao.existsByUrl(url)) {
            throw new LinkAlreadyAddedException(url + " already exists.");
        }
        LinkDTO linkDTO = LinkDTO.builder()
                .url(url)
                .description(description)
                .createdAt(LocalDateTime.now())
                .lastCheckTime(LocalDateTime.now())
                .lastUpdateTime(LocalDateTime.now())
                .build();
        Long linkId = linkDao.add(linkDTO);
        registerActiveLinksMetrics();
        return linkDao.findById(linkId);
    }

    @Override
    public void remove(String url) {
        if (!linkDao.existsByUrl(url)) {
            throw new LinkNotFoundException(url + " not found.");
        }
        linkDao.remove(url);
        registerActiveLinksMetrics();
    }

    @Override
    public void update(LinkDTO link) {
        if (linkDao.findById(link.getLinkId()) == null) {
            throw new LinkNotFoundException("Link with ID " + link.getLinkId() + " not found.");
        }
        linkDao.update(link);
    }

    @Override
    public LinkDTO findById(Long linkId) {
        LinkDTO link = linkDao.findById(linkId);
        if (link == null) {
            throw new LinkNotFoundException("Link with ID " + linkId + " not found.");
        }
        return link;
    }

    @Override
    public LinkDTO findByUrl(String url) {
        LinkDTO link = linkDao.findByUrl(url);
        if (link == null) {
            throw new LinkNotFoundException(url + " not found.");
        }
        return link;
    }

    @Override
    public Collection<LinkDTO> listAll() {
        return linkDao.findAll();
    }

    @Override
    public Collection<LinkDTO> findLinksToCheck(LocalDateTime sinceTime, int offset, int limit) {
        return linkDao.findLinksNotCheckedSince(sinceTime, offset, limit);
    }

    @Override
    public void addTagToLink(Long linkId, String tagName) {
        if (linkDao.findById(linkId) == null) {
            throw new LinkNotFoundException("Link with ID " + linkId + " not found.");
        }
        linkDao.addTagToLink(linkId, tagName);
    }

    @Override
    public void removeTagFromLink(Long linkId, String tagName) {
        if (linkDao.findById(linkId) == null) {
            throw new LinkNotFoundException("Link with ID " + linkId + " not found.");
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
