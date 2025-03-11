package backend.academy.bot.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
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
public class LinkUpdateRequest {
    private Long id;

    @NotBlank(message = "URL cannot be empty")
    private String url;

    private String description;
    private String updateType;
    private List<Long> tgChatIds;
}
