package backend.academy.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ListLinksResponse {
    @JsonProperty("links")
    private List<LinkResponse> links;

    @JsonProperty("size")
    private int size;

    public ListLinksResponse(List<LinkResponse> links, int size) {
        this.links = links;
        this.size = size;
    }

    public List<LinkResponse> getLinks() {
        return links;
    }

    public int getSize() {
        return size;
    }
}
