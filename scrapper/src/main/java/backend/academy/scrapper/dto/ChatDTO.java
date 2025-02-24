package backend.academy.scrapper.dto;

import java.time.LocalDateTime;

public class ChatDTO {
    private final long chatId;
    private final LocalDateTime createdAt;

    public ChatDTO(long chatId, LocalDateTime createdAt) {
        this.chatId = chatId;
        this.createdAt = createdAt;
    }

    public long getChatId() {
        return chatId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
