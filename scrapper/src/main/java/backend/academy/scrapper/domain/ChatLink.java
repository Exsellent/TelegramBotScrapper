package backend.academy.scrapper.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_link")
@IdClass(ChatLinkId.class)
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
    private String filters;
}
