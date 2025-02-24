package backend.academy.scrapper.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class ChatLinkDTO {
    private final Long chatId;
    private final Long linkId;
    private final LocalDateTime sharedAt;

    public Long getChatId() {
        return chatId;
    }

    public Long getLinkId() {
        return linkId;
    }

    public LocalDateTime getSharedAt() {
        return sharedAt;
    }

    // Дополнительный конструктор, если `sharedAt` можно оставить текущим временем
    public ChatLinkDTO(Long chatId, Long linkId) {
        this.chatId = chatId;
        this.linkId = linkId;
        this.sharedAt = LocalDateTime.now(); // Устанавливаем текущее время по умолчанию
    }
}
