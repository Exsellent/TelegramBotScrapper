package backend.academy.scrapper.database.jdbc.service;

import backend.academy.scrapper.dao.LinkDao;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.exception.LinkAlreadyAddedException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.service.LinkService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
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
        return link;
    }

    @Override
    public void remove(String url) {
        if (!linkDao.existsByUrl(url)) {
            throw new LinkNotFoundException(url + NOT_FOUND);
        }
        linkDao.remove(url);
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
}
