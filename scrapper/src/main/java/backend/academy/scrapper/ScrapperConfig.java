package backend.academy.scrapper;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "scrapper.bot")
@Getter
@Setter
public class ScrapperConfig {
    private String databaseAccessType;

    private String githubToken;

    private StackOverflowCredentials stackOverflow;

    @Getter
    @Setter
    public static class StackOverflowCredentials {
        @NotEmpty
        private String key;

        @NotEmpty
        private String accessToken;
    }
}
