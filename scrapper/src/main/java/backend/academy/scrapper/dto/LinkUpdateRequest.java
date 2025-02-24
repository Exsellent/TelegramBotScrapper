package backend.academy.scrapper.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

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

    // Конструктор без аргументов (нужен для Jackson)
    public LinkUpdateRequest() {}

    // Конструктор с аргументами
    public LinkUpdateRequest(Long id, String url, String description, String updateType, List<Long> tgChatIds) {
        this.id = id;
        this.url = url;
        this.description = description;
        this.updateType = updateType;
        this.tgChatIds = tgChatIds;
    }

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
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
