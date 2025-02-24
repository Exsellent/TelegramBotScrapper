package backend.academy.scrapper.dao;

import backend.academy.scrapper.dto.ChatLinkDTO;
import java.util.List;

public interface ChatLinkDao {
    void add(ChatLinkDTO chatLink);

    void remove(Long chatId, Long linkId);

    List<ChatLinkDTO> findAll();

    List<ChatLinkDTO> getChatsForLink(Long linkId);
}
