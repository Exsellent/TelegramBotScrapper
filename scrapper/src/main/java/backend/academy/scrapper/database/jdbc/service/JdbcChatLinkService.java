package backend.academy.scrapper.database.jdbc.service;

import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.dao.ChatLinkDao;
import backend.academy.scrapper.domain.ChatLink;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.service.ChatLinkService;
import java.time.LocalDateTime;
import java.util.Collection;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "jdbc")
@Primary
public class JdbcChatLinkService implements ChatLinkService {

    private final ChatLinkDao chatLinkDao;
    private final ChatDao chatDao;

    @Override
    public void addLinkToChat(long chatId, long linkId) throws ChatNotFoundException {
        if (!chatDao.existsById(chatId)) {
            throw new ChatNotFoundException("Chat with ID " + chatId + " not found.");
        }
        ChatLink chatLink = new ChatLink();
        chatLink.setChatId(chatId);
        chatLink.setLinkId(linkId);
        chatLink.setSharedAt(LocalDateTime.now());
        chatLinkDao.add(chatLink);
    }

    @Override
    public void removeLinkFromChat(long chatId, long linkId) {
        chatLinkDao.removeByChatIdAndLinkId(chatId, linkId);
    }

    @Override
    public Collection<ChatLinkDTO> findAllLinksForChat(long chatId) {
        return chatLinkDao.findByChatId(chatId).stream()
                .map(this::toChatLinkDTO)
                .toList();
    }

    @Override
    public Collection<ChatLinkDTO> findAllChatsForLink(long linkId) {
        return chatLinkDao.findByLinkId(linkId).stream()
                .map(this::toChatLinkDTO)
                .toList();
    }

    @Override
    public boolean existsChatsForLink(long linkId) {
        return chatLinkDao.existsByLinkId(linkId);
    }

    private ChatLinkDTO toChatLinkDTO(ChatLink chatLink) {
        return new ChatLinkDTO(chatLink.getChatId(), chatLink.getLinkId(), chatLink.getSharedAt());
    }
}
