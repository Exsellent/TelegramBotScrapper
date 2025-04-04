package backend.academy.scrapper.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "chat_link")
@IdClass(ChatLinkId.class) // Предполагается, что есть класс для составного ключа
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatLink {
    @Id
    private Long chatId;

    @Id
    private Long linkId;

    @Column(name = "shared_at")
    private LocalDateTime sharedAt;

    @Column(name = "filters", columnDefinition = "json")
    private Map<String, String> filters;
}
