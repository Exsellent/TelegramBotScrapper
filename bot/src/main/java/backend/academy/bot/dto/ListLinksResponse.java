package backend.academy.bot.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ListLinksResponse {
    private List<LinkResponse> links;
    private Integer size;

    public ListLinksResponse(List<LinkResponse> links) {
        this.links = links;
        this.size = links != null ? links.size() : 0;
    }

    public List<LinkResponse> getLinks() {
        return links;
    }

    public Integer getSize() {
        return size;
    }
}
