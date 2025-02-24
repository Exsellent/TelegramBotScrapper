package backend.academy.scrapper.dto;

public class RemoveLinkRequest {
    private String link;

    private RemoveLinkRequest(Builder builder) {
        this.link = builder.link;
    }

    public String getLink() {
        return link;
    }

    public static class Builder {
        private String link;

        public Builder link(String link) {
            this.link = link;
            return this;
        }

        public RemoveLinkRequest build() {
            return new RemoveLinkRequest(this);
        }
    }
}
