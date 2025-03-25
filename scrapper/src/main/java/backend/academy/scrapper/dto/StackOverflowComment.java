package backend.academy.scrapper.dto;

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
public class StackOverflowComment implements Comment {
    @JsonProperty("body")
    private String body;

    @JsonProperty("creation_date")
    private OffsetDateTime createdAt;

    @JsonProperty("edited_date")
    private OffsetDateTime updatedAt;

    @JsonProperty("owner")
    private User owner;

    @Override
    public String getCommentDescription() {
        return body;
    }

    @Override
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public OffsetDateTime getUpdatedAt() {
        return updatedAt != null ? updatedAt : createdAt;
    }

    @Override
    public User getUser() {
        return owner;
    }
}
