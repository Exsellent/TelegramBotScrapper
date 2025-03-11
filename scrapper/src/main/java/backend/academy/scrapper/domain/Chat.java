package backend.academy.scrapper.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "chat")
@Getter
@Setter
@ToString
public class Chat {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
