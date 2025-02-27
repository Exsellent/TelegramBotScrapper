package backend.academy.bot.insidebot;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import backend.academy.bot.configuration.BackoffSettings;

@Configuration
@ConfigurationProperties(prefix = "bot")
@Validated
public class BotService {
    @NotNull
    private RateLimiting rateLimiting = new RateLimiting();

    @NotNull
    private BackoffSettings backoff = new BackoffSettings();

    public static class RateLimiting {
        private int capacity = 20;
        private int tokens = 20;
        private int refillDuration = 60;

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

    public RateLimiting getRateLimiting() {
        return rateLimiting;
    }

    public void setRateLimiting(RateLimiting rateLimiting) {
        this.rateLimiting = rateLimiting;
    }

    public BackoffSettings getBackoff() {
        return backoff;
    }

    public void setBackoff(BackoffSettings backoff) {
        this.backoff = backoff;
    }
}
