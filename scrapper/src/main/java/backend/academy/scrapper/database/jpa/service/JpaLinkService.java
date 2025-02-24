package backend.academy.scrapper.database.jpa.service;

import backend.academy.scrapper.domain.Link;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.exception.LinkAlreadyAddedException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.repository.repository.LinkRepository;
import backend.academy.scrapper.service.LinkService;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

public class JpaLinkService implements LinkService {

    private static final String NOT_FOUND = " not found.";
    private static final String LINK_WITH_ID = "Link with ID ";
    private static final String LINK_WITH_URL = "Link with URL ";

    private final LinkRepository linkRepository;

    public JpaLinkService(LinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    @Override
    @Transactional
    public LinkDTO add(String url, String description) throws LinkAlreadyAddedException {
        linkRepository.findByUrl(url).ifPresent(link -> {
            throw new LinkAlreadyAddedException(LINK_WITH_URL + url + " already exists.");
        });

        Link link = new Link();
        link.setUrl(url);
        link.setDescription(description);
        link.setCreatedAt(LocalDateTime.now());
        link = linkRepository.save(link);
        return new LinkDTO(
                link.getLinkId(),
                link.getUrl(),
                link.getDescription(),
                link.getCreatedAt(),
                link.getLastCheckTime(),
                link.getLastUpdateTime());
    }

    @Override
    @Transactional
    public void remove(String url) {
        Link link = linkRepository
                .findByUrl(url)
                .orElseThrow(() -> new LinkNotFoundException(LINK_WITH_URL + url + NOT_FOUND));
        linkRepository.delete(link);
    }

    @Override
    public Collection<LinkDTO> listAll() {
        return linkRepository.findAll().stream()
                .map(link -> new LinkDTO(
                        link.getLinkId(),
                        link.getUrl(),
                        link.getDescription(),
                        link.getCreatedAt(),
                        link.getLastCheckTime(),
                        link.getLastUpdateTime()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void update(LinkDTO linkDTO) {
        Link link = linkRepository
                .findById(linkDTO.getLinkId())
                .orElseThrow(() -> new LinkNotFoundException(LINK_WITH_ID + linkDTO.getLinkId() + NOT_FOUND));

        link.setUrl(linkDTO.getUrl());
        link.setDescription(linkDTO.getDescription());
        link.setLastCheckTime(linkDTO.getLastCheckTime());
        link.setLastUpdateTime(linkDTO.getLastUpdateTime());
        linkRepository.save(link);
    }

    @Override
    public LinkDTO findById(Long linkId) {
        return linkRepository
                .findById(linkId)
                .map(link -> new LinkDTO(
                        link.getLinkId(),
                        link.getUrl(),
                        link.getDescription(),
                        link.getCreatedAt(),
                        link.getLastCheckTime(),
                        link.getLastUpdateTime()))
                .orElseThrow(() -> new LinkNotFoundException(LINK_WITH_ID + linkId + NOT_FOUND));
    }

    @Override
    public LinkDTO findByUrl(String linkUrl) {
        return linkRepository
                .findByUrl(linkUrl)
                .map(link -> new LinkDTO(
                        link.getLinkId(),
                        link.getUrl(),
                        link.getDescription(),
                        link.getCreatedAt(),
                        link.getLastCheckTime(),
                        link.getLastUpdateTime()))
                .orElseThrow(() -> new LinkNotFoundException(LINK_WITH_URL + linkUrl + NOT_FOUND));
    }

    @Override
    public Collection<LinkDTO> findLinksToCheck(LocalDateTime sinceTime) {
        return linkRepository.findByLastCheckTimeBeforeOrLastCheckTimeIsNull(sinceTime).stream()
                .map(link -> new LinkDTO(
                        link.getLinkId(),
                        link.getUrl(),
                        link.getDescription(),
                        link.getCreatedAt(),
                        link.getLastCheckTime(),
                        link.getLastUpdateTime()))
                .collect(Collectors.toList());
    }
}
