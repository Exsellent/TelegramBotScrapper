package backend.academy.scrapper.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Setter;

@Entity
@Table(name = "chat")
@Setter
public class Chat {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getChatId() {
        return chatId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
