package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.LinkDTO;
import java.time.LocalDateTime;
import java.util.Collection;

public interface LinkService {

    LinkDTO add(String url, String description);

    void remove(String url);

    Collection<LinkDTO> listAll();

    void update(LinkDTO link);

    Collection<LinkDTO> findLinksToCheck(LocalDateTime sinceTime);

    LinkDTO findById(Long linkId);

    LinkDTO findByUrl(String linkUrl);
}
