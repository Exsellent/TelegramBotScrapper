package backend.academy.bot.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limiting")
public class RateLimitingProperties {
    private int capacity;
    private int tokens;
    private int refillDuration;

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getTokens() {
        return tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public int getRefillDuration() {
        return refillDuration;
    }

    public void setRefillDuration(int refillDuration) {
        this.refillDuration = refillDuration;
    }
}
