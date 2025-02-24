package backend.academy.scrapper.dao;

import backend.academy.scrapper.dto.LinkDTO;
import java.time.LocalDateTime;
import java.util.List;

public interface LinkDao {
    Long add(LinkDTO link);

    void remove(Long linkId);

    void remove(String url);

    List<LinkDTO> findAll();

    void update(LinkDTO link);

    List<LinkDTO> findLinksNotCheckedSince(LocalDateTime dateTime);

    boolean existsByUrl(String url);

    LinkDTO findById(Long linkId);

    LinkDTO findByUrl(String linkUrl);
}
