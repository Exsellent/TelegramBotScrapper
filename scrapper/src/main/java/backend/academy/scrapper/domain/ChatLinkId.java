package backend.academy.scrapper.domain;

import java.io.Serializable;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatLinkId implements Serializable {
    private Long chatId;
    private Long linkId;
}
