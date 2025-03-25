package backend.academy.scrapper.repository.repository.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class ChatDTO {
    private final long chatId;
    private final LocalDateTime createdAt;
}
