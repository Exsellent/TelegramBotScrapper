package backend.academy.scrapper.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    private Long id;

    @NotEmpty
    private String url;

    @NotEmpty
    private String description;

    @NotEmpty
    private String updateType;

    @NotEmpty
    private List<Long> tgChatIds;
}
