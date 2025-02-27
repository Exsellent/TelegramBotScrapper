package backend.academy.bot.configuration;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "backoff")
@Getter
@Setter
public class BackOffProperties {
    private BackoffSettings settings = new BackoffSettings();
    private List<Integer> retryableStatusCodes;

    public BackoffSettings getSettings() {
        return settings;
    }

    public void setSettings(BackoffSettings settings) {
        this.settings = settings;
    }

    public long getInitialDelay() {
        return settings.getInitialDelay();
    }

    public double getMultiplier() {
        return settings.getMultiplier();
    }

    public long getIncrement() {
        return settings.getIncrement();
    }

    public String getStrategy() {
        return settings.getStrategy();
    }

    public int getMaxAttempts() {
        return settings.getMaxAttempts();
    }

    public List<Integer> getRetryableStatusCodes() {
        return retryableStatusCodes;
    }

    public void setRetryableStatusCodes(List<Integer> retryableStatusCodes) {
        this.retryableStatusCodes = retryableStatusCodes;
    }
}
