package backend.academy.scrapper.configuration;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limiting")
@Getter
public class RateLimitingProperties {
    private int capacity;
    private int tokens;
    private int refillDuration;

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public void setRefillDuration(int refillDuration) {
        this.refillDuration = refillDuration;
    }
}
