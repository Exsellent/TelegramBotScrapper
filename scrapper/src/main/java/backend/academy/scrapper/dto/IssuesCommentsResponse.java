package backend.academy.scrapper.repository.repository.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssuesCommentsResponse implements Comment {
    private String url;
    private Long id;
    private String body;

    @JsonProperty("user") // Добавлено: маппинг поля "user" из JSON
    private User user; // Добавлено

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    @Override
    public String getCommentDescription() {
        return body;
    }

    // Добавлена реализация метода из интерфейса Comment
    @Override
    public User getUser() {
        return user;
    }
}
