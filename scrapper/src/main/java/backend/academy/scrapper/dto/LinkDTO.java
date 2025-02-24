package backend.academy.scrapper.dto;

import java.time.LocalDateTime;

public class LinkDTO {
    private Long linkId;
    private String url;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime lastCheckTime;
    private LocalDateTime lastUpdateTime;

    public LinkDTO(
            Long linkId,
            String url,
            String description,
            LocalDateTime createdAt,
            LocalDateTime lastCheckTime,
            LocalDateTime lastUpdateTime) {
        this.linkId = linkId;
        this.url = url;
        this.description = description;
        this.createdAt = createdAt;
        this.lastCheckTime = lastCheckTime;
        this.lastUpdateTime = lastUpdateTime;
    }

    public Long getLinkId() {
        return linkId;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastCheckTime(LocalDateTime lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public void setLinkId(Long linkId) {
        this.linkId = linkId;
    }
}
