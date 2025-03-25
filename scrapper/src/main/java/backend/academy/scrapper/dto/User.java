package backend.academy.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    @JsonProperty("id") // Для GitHub
    private Long id;

    private String login; // Для GitHub

    @JsonProperty("user_id") // Для StackOverflow
    private Long userId;

    @JsonProperty("display_name")
    private String displayName; // Для StackOverflow

    // Метод для получения имени пользователя в зависимости от источника
    public String getName() {
        return login != null ? login : displayName;
    }

    // Метод для получения ID в зависимости от источника
    public Long getId() {
        return id != null ? id : userId;
    }
}
