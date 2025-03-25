package backend.academy.scrapper.domain;

import java.io.Serializable;
import java.util.Objects;

public class ChatLinkId implements Serializable {

    private Long chatId;
    private Long linkId;

    public ChatLinkId() {}

    public ChatLinkId(Long chatId, Long linkId) {
        this.chatId = chatId;
        this.linkId = linkId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getLinkId() {
        return linkId;
    }

    public void setLinkId(Long linkId) {
        this.linkId = linkId;
    }

    // Переопределение equals и hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatLinkId that = (ChatLinkId) o;
        return Objects.equals(chatId, that.chatId) && Objects.equals(linkId, that.linkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, linkId);
    }
}
