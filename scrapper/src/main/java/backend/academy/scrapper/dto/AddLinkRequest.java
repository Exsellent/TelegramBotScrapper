package backend.academy.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AddLinkRequest {
    @JsonProperty("link")
    private String link;

    @JsonProperty("description")
    private String description;

    public AddLinkRequest() {}

    public AddLinkRequest(String link, String description) {
        this.link = link;
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
