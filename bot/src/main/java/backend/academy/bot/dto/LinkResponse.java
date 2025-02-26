package backend.academy.bot.dto;

public class LinkResponse {
    private Long id;
    private String url;
    private String description;

    public LinkResponse() {
        // Пустой конструктор для JSON десериализации
    }

    public LinkResponse(Long id, String url) {
        this.id = id;
        this.url = url;
    }

    public LinkResponse(Long id, String url, String description) {
        this.id = id;
        this.url = url;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
