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
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnswerResponse {

    @JsonProperty("answer_id")
    private Long answerId;

    @JsonProperty("creation_date")
    private OffsetDateTime creationDate;

    @JsonProperty("last_activity_date")
    private OffsetDateTime lastActivityDate;

    @JsonProperty("question_id")
    private Long questionId;

    public Long getAnswerId() {
        return answerId;
    }

    public OffsetDateTime getLastActivityDate() {
        return lastActivityDate;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }
}
