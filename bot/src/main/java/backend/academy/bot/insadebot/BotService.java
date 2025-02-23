package backend.academy.bot.insadebot;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "bot")
@Validated
public class BotService {
    @NotNull
    private RateLimiting rateLimiting = new RateLimiting();

    @NotNull
    private Backoff backoff = new Backoff();

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

    public static class Backoff {
        private String strategy = "exponential";
        private long initialDelay = 1000;
        private double multiplier = 2.0;
        private long increment = 1000;
        private int maxAttempts = 3;

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public long getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(long initialDelay) {
            this.initialDelay = initialDelay;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public long getIncrement() {
            return increment;
        }

        public void setIncrement(long increment) {
            this.increment = increment;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }

    public RateLimiting getRateLimiting() {
        return rateLimiting;
    }

    public void setRateLimiting(RateLimiting rateLimiting) {
        this.rateLimiting = rateLimiting;
    }

    public Backoff getBackoff() {
        return backoff;
    }

    public void setBackoff(Backoff backoff) {
        this.backoff = backoff;
    }
}
