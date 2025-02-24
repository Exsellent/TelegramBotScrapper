package backend.academy.scrapper.dao;

import backend.academy.scrapper.dto.ChatDTO;
import java.util.List;

public interface ChatDao {

    void add(ChatDTO chat);

    void remove(Long chatId);

    List<ChatDTO> findAll();

    boolean existsById(Long chatId);
}
