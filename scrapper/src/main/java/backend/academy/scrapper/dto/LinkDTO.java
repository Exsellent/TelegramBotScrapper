package backend.academy.scrapper.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LinkDTO {
    private Long linkId;
    private String url;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime lastCheckTime;
    private LocalDateTime lastUpdateTime;
    private List<String> tags;

    @Override
    public String toString() {
        return "LinkDTO(" + "linkId="
                + linkId + ", url="
                + url + ", description="
                + description + ", createdAt="
                + createdAt + ", lastCheckTime="
                + lastCheckTime + ", lastUpdateTime="
                + lastUpdateTime + ", tags="
                + tags + ')';
    }
}
