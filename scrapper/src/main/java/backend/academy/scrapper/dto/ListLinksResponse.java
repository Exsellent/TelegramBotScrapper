package backend.academy.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ListLinksResponse {

    @JsonProperty("links")
    private List<LinkResponse> links;

    @JsonProperty("size")
    private int size;
}
