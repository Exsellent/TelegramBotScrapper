package backend.academy.scrapper.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ChatLinkDTO {
    private final long chatId;
    private final long linkId;
    private final LocalDateTime sharedAt; // Возвращаем поле
    private final Map<String, String> filters;

    public ChatLinkDTO(long chatId, long linkId, Map<String, String> filters, LocalDateTime sharedAt) {
        this.chatId = chatId;
        this.linkId = linkId;
        this.filters = filters != null ? Map.copyOf(filters) : Map.of();
        this.sharedAt = sharedAt;
    }

    public ChatLinkDTO(long chatId, long linkId) {
        this(chatId, linkId, null, null);
    }

    public long getChatId() { return chatId; }
    public long getLinkId() { return linkId; }
    public LocalDateTime getSharedAt() { return sharedAt; }
    public Map<String, String> getFilters() { return filters; }
}
