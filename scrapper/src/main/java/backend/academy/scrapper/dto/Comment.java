package backend.academy.scrapper.dto;

import java.time.OffsetDateTime;

public interface Comment {

    String getCommentDescription();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();
}
