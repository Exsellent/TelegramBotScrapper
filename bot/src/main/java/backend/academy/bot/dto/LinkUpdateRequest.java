package backend.academy.bot.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter


public class LinkUpdateRequest {
    private Long id;
    @NotBlank(message = "URL cannot be empty")

    private String url;
    private String description;
    private String updateType;
    private List<Long> tgChatIds;

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getDescription() {
        return description;
    }

    public String getUpdateType() {
        return updateType;
    }

    public List<Long> getTgChatIds() {
        return tgChatIds;
    }
}
