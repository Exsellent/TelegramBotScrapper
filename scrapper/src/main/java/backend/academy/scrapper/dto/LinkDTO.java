package backend.academy.scrapper.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LinkDTO {
    private Long linkId;
    private String url;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime lastCheckTime;
    private LocalDateTime lastUpdateTime;
}
