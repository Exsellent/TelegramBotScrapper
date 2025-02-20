package backend.academy.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class AddLinkRequest {

    @JsonProperty("link")
    private String link;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("filters")
    private Map<String, String> filters;

    public AddLinkRequest() {
        // Пустой конструктор для JSON десериализации
    }

    public AddLinkRequest(String link, List<String> tags, Map<String, String> filters) {
        this.link = link;
        this.tags = tags;
        this.filters = filters;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, String> filters) {
        this.filters = filters;
    }

    @Override
    public String toString() {
        return "AddLinkRequest{" + "link='" + link + '\'' + ", tags=" + tags + ", filters=" + filters + '}';
    }
}
