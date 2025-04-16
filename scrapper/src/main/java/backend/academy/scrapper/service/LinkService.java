package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.exception.LinkAlreadyAddedException;
import java.time.LocalDateTime;
import java.util.Collection;

public interface LinkService {
    LinkDTO add(String url, String description) throws LinkAlreadyAddedException;

    void remove(String url);

    Collection<LinkDTO> listAll();

    void update(LinkDTO linkDTO);

    LinkDTO findById(Long linkId);

    LinkDTO findByUrl(String linkUrl);

    Collection<LinkDTO> findLinksToCheck(LocalDateTime sinceTime, int offset, int limit);

    void addTagToLink(Long linkId, String tagName);

    void removeTagFromLink(Long linkId, String tagName);

    Collection<LinkDTO> findLinksByTag(String tagName);
}
