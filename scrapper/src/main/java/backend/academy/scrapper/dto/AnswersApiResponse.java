package backend.academy.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnswersApiResponse {
    private List<AnswerResponse> items;

    public List<AnswerResponse> getItems() {
        return items;
    }
}
