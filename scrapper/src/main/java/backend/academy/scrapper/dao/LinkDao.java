package backend.academy.scrapper.dao;

import backend.academy.scrapper.dto.LinkDTO;
import java.time.LocalDateTime;
import java.util.Collection;

public interface LinkDao {
    Long add(LinkDTO link);

    void remove(String url);

    Collection<LinkDTO> findAll();

    void update(LinkDTO link);

    LinkDTO findById(Long linkId);

    LinkDTO findByUrl(String url);

    Collection<LinkDTO> findLinksNotCheckedSince(LocalDateTime sinceTime, int offset, int limit);

    boolean existsByUrl(String url);

    void addTagToLink(Long linkId, String tagName);

    void removeTagFromLink(Long linkId, String tagName);

    Collection<LinkDTO> findLinksByTag(String tagName);
}
