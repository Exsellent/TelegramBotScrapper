package backend.academy.scrapper.configuration;

import java.time.Duration;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

@Configuration
@EnableConfigurationProperties(RetryProperties.class)
@AllArgsConstructor
public class RetryConfig {

    private final RetryProperties retryProperties;

    @Bean
    @ConditionalOnProperty(name = "retry.strategy", havingValue = "exponential", matchIfMissing = true)
    public Retry exponentialRetrySpec() {
        return Retry.backoff(
                        retryProperties.getMaxAttempts(), Duration.ofSeconds(retryProperties.getFirstBackoffSeconds()))
                .maxBackoff(Duration.ofSeconds(retryProperties.getMaxBackoffSeconds()))
                .jitter(retryProperties.getJitterFactor())
                .filter(retryFilter());
    }

    @Bean
    @ConditionalOnProperty(name = "retry.strategy", havingValue = "constant")
    public Retry constantRetrySpec() {
        return Retry.fixedDelay(
                        retryProperties.getMaxAttempts(), Duration.ofSeconds(retryProperties.getFirstBackoffSeconds()))
                .filter(retryFilter());
    }

    @Bean
    public Predicate<Throwable> retryFilter() {
        return throwable -> throwable instanceof WebClientResponseException
                && retryProperties
                        .getRetryableStatusCodes()
                        .contains(((WebClientResponseException) throwable)
                                .getStatusCode()
                                .value());
    }
}
