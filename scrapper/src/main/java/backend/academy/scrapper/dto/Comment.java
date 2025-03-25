package backend.academy.scrapper.repository.repository.dto;

import java.time.OffsetDateTime;

public interface Comment {

    String getCommentDescription();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();

    User getUser();
}
