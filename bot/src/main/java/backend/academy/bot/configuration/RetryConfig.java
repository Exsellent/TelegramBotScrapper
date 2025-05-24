package backend.academy.bot.configuration;

import backend.academy.bot.exception.ApiException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

@Configuration
public class RetryConfig {

    @Value("${retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${retry.first-backoff-seconds:1}")
    private long backoffSeconds;

    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(maxAttempts)
                .fixedBackoff(backoffSeconds * 1000)
                .retryOn(ApiException.class)
                .build();
    }

    @Bean
    public SimpleClientHttpRequestFactory retryRequestFactory() { // Переименовано
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return factory;
    }

    @Bean
    public RestClient restClient(SimpleClientHttpRequestFactory retryRequestFactory) {

        return RestClient.builder()
                .requestFactory(retryRequestFactory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, (request, response) -> {
                    if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        throw new ApiException("Rate limit exceeded: " + response.getStatusCode());
                    }
                    throw new ApiException("Client error: " + response.getStatusCode());
                })
                .defaultStatusHandler(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ApiException("Server error: " + response.getStatusCode());
                })
                .build();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .build();
        return CircuitBreakerRegistry.of(config);
    }
}
