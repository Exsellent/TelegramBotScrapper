package backend.academy.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Универсальное представление пользователя для GitHub и StackOverflow. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    /** GitHub: ID пользователя */
    @JsonProperty("id")
    private Long id;

    /** GitHub: логин */
    private String login;

    /** StackOverflow: ID пользователя */
    @JsonProperty("user_id")
    private Long userId;

    /** StackOverflow: отображаемое имя */
    @JsonProperty("display_name")
    private String displayName;

    /**
     * Возвращает имя пользователя: login (GitHub) или displayName (StackOverflow).
     *
     * @return логин или имя пользователя
     */
    public String getName() {
        return login != null ? login : displayName;
    }

    /**
     * Возвращает ID пользователя: id (GitHub) или userId (StackOverflow).
     *
     * @return ID пользователя
     */
    public Long getId() {
        return id != null ? id : userId;
    }
}
