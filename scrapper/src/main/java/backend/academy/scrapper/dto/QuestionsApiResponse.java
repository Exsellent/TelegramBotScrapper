package backend.academy.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionsApiResponse {
    private List<QuestionResponse> items;

    public List<QuestionResponse> getItems() {
        return items;
    }
}
