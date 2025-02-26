package backend.academy.scrapper.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class RemoveLinkRequest {
    private String link;

    @Builder
    public RemoveLinkRequest(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }
}
