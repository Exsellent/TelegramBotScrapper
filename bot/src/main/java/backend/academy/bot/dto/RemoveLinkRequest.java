package backend.academy.bot.dto;

public class RemoveLinkRequest {
    private final String link;

    public RemoveLinkRequest(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String link;

        private Builder() {
        }

        public Builder link(String link) {
            this.link = link;
            return this;
        }

        public RemoveLinkRequest build() {
            return new RemoveLinkRequest(this.link);
        }
    }
}
