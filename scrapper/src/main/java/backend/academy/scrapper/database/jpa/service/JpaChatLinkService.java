package backend.academy.scrapper.database.jpa.service;

import backend.academy.scrapper.domain.ChatLink;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.repository.repository.ChatLinkRepository;
import backend.academy.scrapper.repository.repository.ChatRepository;
import backend.academy.scrapper.service.ChatLinkService;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@AllArgsConstructor
public class JpaChatLinkService implements ChatLinkService {

    private final ChatLinkRepository chatLinkRepository;
    private final ChatRepository chatRepository;

    public void addLinkToChat(long chatId, long linkId) throws ChatNotFoundException {
        if (!chatRepository.existsById(chatId)) {
            throw new ChatNotFoundException("Chat with ID " + chatId + " not found.");
        }
        ChatLink chatLink = new ChatLink();
        chatLink.setChatId(chatId);
        chatLink.setLinkId(linkId);
        chatLink.setSharedAt(LocalDateTime.now());
        chatLinkRepository.save(chatLink);
    }

    public void removeLinkFromChat(long chatId, long linkId) {
        chatLinkRepository.deleteByChatIdAndLinkId(chatId, linkId);
    }

    @Override
    public Collection<ChatLinkDTO> findAllLinksForChat(long chatId) {
        return chatLinkRepository.findByChatId(chatId).stream()
                .map(chatLink -> new ChatLinkDTO(chatLink.getChatId(), chatLink.getLinkId(), chatLink.getSharedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ChatLinkDTO> findAllChatsForLink(long linkId) {
        return chatLinkRepository.findByLinkId(linkId).stream()
                .map(chatLink -> new ChatLinkDTO(chatLink.getChatId(), chatLink.getLinkId(), chatLink.getSharedAt()))
                .collect(Collectors.toList());
    }

    public boolean existsChatsForLink(long linkId) {
        return chatLinkRepository.existsByLinkId(linkId);
    }
}
