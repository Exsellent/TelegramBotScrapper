package backend.academy.scrapper.database.dao.jpa;

import backend.academy.scrapper.dao.LinkDao;
import backend.academy.scrapper.domain.Link;
import backend.academy.scrapper.domain.Tag;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.repository.repository.LinkRepository;
import backend.academy.scrapper.repository.repository.TagRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "jpa")
@Primary
public class JpaLinkDao implements LinkDao {

    private final LinkRepository linkRepository;
    private final TagRepository tagRepository;

    @Override
    public Long add(LinkDTO linkDTO) {
        Link link = new Link();
        link.setUrl(linkDTO.getUrl());
        link.setDescription(linkDTO.getDescription());
        link.setCreatedAt(LocalDateTime.now());
        link.setLastCheckTime(linkDTO.getLastCheckTime());
        link.setLastUpdateTime(linkDTO.getLastUpdateTime());
        Link savedLink = linkRepository.save(link);
        return savedLink.getId();
    }

    @Override
    public void remove(String url) {
        Optional<Link> linkOptional = linkRepository.findByUrl(url);
        linkOptional.ifPresent(linkRepository::delete);
    }

    @Override
    public Collection<LinkDTO> findAll() {
        return linkRepository.findAll().stream().map(this::toLinkDTO).collect(Collectors.toList());
    }

    @Override
    public void update(LinkDTO linkDTO) {
        Optional<Link> linkOptional = linkRepository.findById(linkDTO.getLinkId());
        if (linkOptional.isPresent()) {
            Link link = linkOptional.get();
            link.setUrl(linkDTO.getUrl());
            link.setDescription(linkDTO.getDescription());
            link.setLastCheckTime(linkDTO.getLastCheckTime());
            link.setLastUpdateTime(linkDTO.getLastUpdateTime());
            linkRepository.save(link);
        }
    }

    @Override
    public LinkDTO findById(Long linkId) {
        return linkRepository.findById(linkId).map(this::toLinkDTO).orElse(null);
    }

    @Override
    public LinkDTO findByUrl(String url) {
        return linkRepository.findByUrl(url).map(this::toLinkDTO).orElse(null);
    }

    @Override
    public Collection<LinkDTO> findLinksNotCheckedSince(LocalDateTime sinceTime, int offset, int limit) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        return linkRepository.findByLastCheckTimeBeforeOrLastCheckTimeIsNull(sinceTime, pageable).stream()
                .map(this::toLinkDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByUrl(String url) {
        return linkRepository.existsByUrl(url);
    }

    @Override
    public void addTagToLink(Long linkId, String tagName) {
        Optional<Link> linkOptional = linkRepository.findById(linkId);
        if (linkOptional.isPresent()) {
            Link link = linkOptional.get();
            Optional<Tag> tagOptional = tagRepository.findByName(tagName);
            Tag tag;
            if (tagOptional.isPresent()) {
                tag = tagOptional.get();
            } else {
                tag = new Tag();
                tag.setName(tagName);
                tag = tagRepository.save(tag);
            }
            link.getTags().add(tag);
            linkRepository.save(link);
        }
    }

    @Override
    public void removeTagFromLink(Long linkId, String tagName) {
        Optional<Link> linkOptional = linkRepository.findById(linkId);
        if (linkOptional.isPresent()) {
            Link link = linkOptional.get();
            Optional<Tag> tagOptional = tagRepository.findByName(tagName);
            tagOptional.ifPresent(tag -> {
                link.getTags().remove(tag);
                linkRepository.save(link);
            });
        }
    }

    @Override
    public Collection<LinkDTO> findLinksByTag(String tagName) {
        return linkRepository.findByTags_Name(tagName).stream()
                .map(this::toLinkDTO)
                .collect(Collectors.toList());
    }

    private LinkDTO toLinkDTO(Link link) {
        return LinkDTO.builder()
                .linkId(link.getId())
                .url(link.getUrl())
                .description(link.getDescription())
                .createdAt(link.getCreatedAt())
                .lastCheckTime(link.getLastCheckTime())
                .lastUpdateTime(link.getLastUpdateTime())
                .tags(link.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
                .build();
    }
}
