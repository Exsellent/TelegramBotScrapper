package backend.academy.scrapper.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class ChatLinkDTO {
    private final Long chatId;
    private final Long linkId;
    private final LocalDateTime sharedAt;

    // Дополнительный конструктор, если `sharedAt` можно оставить текущим временем
    public ChatLinkDTO(Long chatId, Long linkId) {
        this.chatId = chatId;
        this.linkId = linkId;
        this.sharedAt = LocalDateTime.now(); // Устанавливаем текущее время по умолчанию
    }
}
