package backend.academy.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AddLinkRequest {

    @JsonProperty("link")
    private String link;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("filters")
    private Map<String, String> filters;
}
