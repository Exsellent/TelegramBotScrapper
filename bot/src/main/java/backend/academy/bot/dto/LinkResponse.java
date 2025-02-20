package backend.academy.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LinkResponse {
    private String url;
    private String description;

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }
}
