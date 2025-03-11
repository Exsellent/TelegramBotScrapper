package backend.academy.scrapper;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bot")
public class ScrapperConfig {

    @NotEmpty
    private String databaseAccessType;

    @NotEmpty
    private String githubToken;

    private StackOverflowCredentials stackOverflow;

    public ScrapperConfig() {}

    public String getDatabaseAccessType() {
        return databaseAccessType;
    }

    public void setDatabaseAccessType(String databaseAccessType) {
        this.databaseAccessType = databaseAccessType;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }

    public StackOverflowCredentials getStackOverflow() {
        return stackOverflow;
    }

    public void setStackOverflow(StackOverflowCredentials stackOverflow) {
        this.stackOverflow = stackOverflow;
    }

    public static class StackOverflowCredentials {

        @NotEmpty
        private String key;

        @NotEmpty
        private String accessToken;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }
}
