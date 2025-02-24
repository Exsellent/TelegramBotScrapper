package backend.academy.scrapper.service;

import backend.academy.scrapper.domain.Link;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.repository.repository.LinkRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class LinkServiceImpl implements LinkService {

    private final LinkRepository linkRepository;
    private final AtomicLong idGenerator = new AtomicLong(1);

    public LinkServiceImpl(LinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    @Override
    public LinkDTO add(String url, String description) {
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
    public void remove(String url) {
        Optional<Link> link = linkRepository.findByUrl(url);
        link.ifPresent(linkRepository::delete);
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
    public void update(LinkDTO linkDTO) {
        Link link =
                linkRepository.findById(linkDTO.getLinkId()).orElseThrow(() -> new RuntimeException("Link not found"));
        link.setUrl(linkDTO.getUrl());
        link.setDescription(linkDTO.getDescription());
        link.setLastCheckTime(linkDTO.getLastCheckTime());
        link.setLastUpdateTime(linkDTO.getLastUpdateTime());
        linkRepository.save(link);
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
                .orElse(null);
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
                .orElse(null);
    }
}
