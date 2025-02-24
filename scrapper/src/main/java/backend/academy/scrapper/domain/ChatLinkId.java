package backend.academy.scrapper.domain;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@EqualsAndHashCode
public class ChatLinkId implements Serializable {
    private Long chatId;
    private Long linkId;

    public Long getChatId() {
        return chatId;
    }

    public Long getLinkId() {
        return linkId;
    }
}
