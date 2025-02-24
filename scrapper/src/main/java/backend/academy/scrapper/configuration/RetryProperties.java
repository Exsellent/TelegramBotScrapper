package backend.academy.scrapper.configuration;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "retry")
@Setter
@Getter
public class RetryProperties {

    private String strategy;
    private long maxAttempts;
    private long firstBackoffSeconds;
    private long maxBackoffSeconds;
    private double jitterFactor;
    private List<Integer> retryableStatusCodes;

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public long getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(long maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getFirstBackoffSeconds() {
        return firstBackoffSeconds;
    }

    public void setFirstBackoffSeconds(long firstBackoffSeconds) {
        this.firstBackoffSeconds = firstBackoffSeconds;
    }

    public long getMaxBackoffSeconds() {
        return maxBackoffSeconds;
    }

    public void setMaxBackoffSeconds(long maxBackoffSeconds) {
        this.maxBackoffSeconds = maxBackoffSeconds;
    }

    public double getJitterFactor() {
        return jitterFactor;
    }

    public void setJitterFactor(double jitterFactor) {
        this.jitterFactor = jitterFactor;
    }

    public List<Integer> getRetryableStatusCodes() {
        return retryableStatusCodes;
    }

    public void setRetryableStatusCodes(List<Integer> retryableStatusCodes) {
        this.retryableStatusCodes = retryableStatusCodes;
    }
}
