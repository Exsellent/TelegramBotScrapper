package backend.academy.scrapper.service;

import backend.academy.scrapper.domain.Link;
import backend.academy.scrapper.domain.Tag;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.repository.repository.LinkRepository;
import backend.academy.scrapper.repository.repository.TagRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "some-other-type", matchIfMissing = false)
public class LinkServiceImpl implements LinkService {

    private final LinkRepository linkRepository;
    private final TagRepository tagRepository;
    private final MeterRegistry meterRegistry;
    private final GitHubService gitHubService; // Добавлено
    private final StackOverflowService stackOverflowService; // Добавлено

    @Autowired
    public LinkServiceImpl(
            LinkRepository linkRepository,
            TagRepository tagRepository,
            MeterRegistry meterRegistry,
            GitHubService gitHubService,
            StackOverflowService stackOverflowService) {
        this.linkRepository = linkRepository;
        this.tagRepository = tagRepository;
        this.meterRegistry = meterRegistry;
        this.gitHubService = gitHubService;
        this.stackOverflowService = stackOverflowService;
    }

    @Override
    public LinkDTO add(String url, String description) {
        Link link = new Link();
        link.setUrl(url);
        link = linkRepository.save(link);
        registerActiveLinksMetrics();
        return LinkDTO.builder()
                .linkId(link.getId())
                .url(link.getUrl())
                .description(description)
                .lastCheckTime(link.getLastCheckTime())
                .tags(link.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
                .build();
    }

    @Override
    public void remove(String url) {
        Optional<Link> link = linkRepository.findByUrl(url);
        link.ifPresent(l -> {
            linkRepository.delete(l);
            registerActiveLinksMetrics();
        });
    }

    @Override
    public Collection<LinkDTO> listAll() {
        return linkRepository.findAll().stream()
                .map(link -> LinkDTO.builder()
                        .linkId(link.getId())
                        .url(link.getUrl())
                        .lastCheckTime(link.getLastCheckTime())
                        .tags(link.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void update(LinkDTO linkDTO) {
        Link link =
                linkRepository.findById(linkDTO.getLinkId()).orElseThrow(() -> new RuntimeException("Link not found"));
        link.setUrl(linkDTO.getUrl());
        link.setLastCheckTime(linkDTO.getLastCheckTime());
        linkRepository.save(link);
    }

    @Override
    public Collection<LinkDTO> findLinksToCheck(LocalDateTime sinceTime, int offset, int limit) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        return linkRepository.findByLastCheckTimeBeforeOrLastCheckTimeIsNull(sinceTime, pageable).stream()
                .map(link -> LinkDTO.builder()
                        .linkId(link.getId())
                        .url(link.getUrl())
                        .lastCheckTime(link.getLastCheckTime())
                        .tags(link.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public LinkDTO findById(Long linkId) {
        return linkRepository
                .findById(linkId)
                .map(link -> LinkDTO.builder()
                        .linkId(link.getId())
                        .url(link.getUrl())
                        .lastCheckTime(link.getLastCheckTime())
                        .tags(link.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
                        .build())
                .orElse(null);
    }

    @Override
    public LinkDTO findByUrl(String linkUrl) {
        return linkRepository
                .findByUrl(linkUrl)
                .map(link -> LinkDTO.builder()
                        .linkId(link.getId())
                        .url(link.getUrl())
                        .lastCheckTime(link.getLastCheckTime())
                        .tags(link.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
                        .build())
                .orElse(null);
    }

    @Override
    public void addTagToLink(Long linkId, String tagName) {
        Link link = linkRepository.findById(linkId).orElseThrow(() -> new RuntimeException("Link not found"));
        Tag tag = tagRepository.findByName(tagName).orElseGet(() -> {
            Tag newTag = new Tag();
            newTag.setName(tagName);
            return tagRepository.save(newTag);
        });
        link.getTags().add(tag);
        linkRepository.save(link);
    }

    @Override
    public void removeTagFromLink(Long linkId, String tagName) {
        Link link = linkRepository.findById(linkId).orElseThrow(() -> new RuntimeException("Link not found"));
        link.getTags().removeIf(tag -> tag.getName().equals(tagName));
        linkRepository.save(link);
    }

    @Override
    public Collection<LinkDTO> findLinksByTag(String tagName) {
        return linkRepository.findByTags_Name(tagName).stream()
                .map(link -> LinkDTO.builder()
                        .linkId(link.getId())
                        .url(link.getUrl())
                        .lastCheckTime(link.getLastCheckTime())
                        .tags(link.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void registerActiveLinksMetrics() {
        List<Link> links = linkRepository.findAll();
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
