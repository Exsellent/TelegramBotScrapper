package backend.academy.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionResponse {

    @JsonProperty("question_id")
    private Long questionId;

    private String title;

    @JsonProperty("last_activity_date")
    private OffsetDateTime lastActivityDate;

    @JsonProperty("creation_date")
    private OffsetDateTime creationDate;

    public Long getQuestionId() {
        return questionId;
    }

    public String getTitle() {
        return title;
    }

    public OffsetDateTime getLastActivityDate() {
        return lastActivityDate;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }
}
