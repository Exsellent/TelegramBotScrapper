package backend.academy.scrapper.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChatLinkDTO {
    private Long chatId;
    private Long linkId;
    private LocalDateTime sharedAt;

    public ChatLinkDTO(Long chatId, Long linkId) {
        this.chatId = chatId;
        this.linkId = linkId;
        this.sharedAt = LocalDateTime.now();
    }
}
