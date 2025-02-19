package backend.academy.bot.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "backoff")
public class BackOffProperties {
    private String strategy = "exponential";
    private long initialDelay = 1000;
    private double multiplier = 2.0;
    private long increment = 1000;
    private int maxAttempts = 5;
    private List<Integer> retryableStatusCodes;

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

    public List<Integer> getRetryableStatusCodes() {
        return retryableStatusCodes;
    }

    public void setRetryableStatusCodes(List<Integer> retryableStatusCodes) {
        this.retryableStatusCodes = retryableStatusCodes;
    }
}
