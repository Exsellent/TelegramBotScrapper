package backend.academy.scrapper.dao;

import backend.academy.scrapper.dto.ChatDTO;
import java.util.Collection;

public interface ChatDao {
    void add(ChatDTO chat);

    void remove(long chatId);

    Collection<ChatDTO> findAll();

    boolean existsById(long chatId);
}
