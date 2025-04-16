package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.dto.LinkResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class LinkManagementService {

    private final ChatLinkService chatLinkService;
    private final LinkService linkService;

    @Transactional
    public LinkResponse removeLinkFromChatAndCleanup(Long tgChatId, String linkUrl) {
        LinkDTO link = linkService.findByUrl(linkUrl);
        chatLinkService.removeLinkFromChat(tgChatId, link.getLinkId());
        if (!chatLinkService.existsChatsForLink(link.getLinkId())) {
            linkService.remove(linkUrl);
        }
        return new LinkResponse(link.getLinkId(), link.getUrl(), link.getDescription());
    }
}
