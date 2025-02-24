package backend.academy.scrapper.database.jpa.jdbc.service;

import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.dao.ChatLinkDao;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.service.ChatLinkService;
import java.time.LocalDateTime;
import java.util.Collection;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class JdbcChatLinkService implements ChatLinkService {

    private final ChatLinkDao chatLinkDao;
    private final ChatDao chatDao;

    @Override
    public void addLinkToChat(long chatId, long linkId) throws ChatNotFoundException {
        if (!chatDao.existsById(chatId)) {
            throw new ChatNotFoundException("Chat with ID " + chatId + " not found.");
        }
        ChatLinkDTO chatLink = new ChatLinkDTO(chatId, linkId, LocalDateTime.now());
        chatLinkDao.add(chatLink);
    }

    @Override
    public void removeLinkFromChat(long chatId, long linkId) {
        chatLinkDao.remove(chatId,
            linkId);
    }

    @Override
    public Collection<ChatLinkDTO> findAllLinksForChat(long chatId) {
        return chatLinkDao.findAll().stream()
                .filter(chatLinkDTO -> chatLinkDTO.getChatId().equals(chatId))
                .toList();
    }

    @Override
    public Collection<ChatLinkDTO> findAllChatsForLink(long linkId) {
        return chatLinkDao.getChatsForLink(linkId);
    }

    @Override
    public boolean existsChatsForLink(long linkId) {
        return !chatLinkDao.getChatsForLink(linkId).isEmpty();
    }
}
