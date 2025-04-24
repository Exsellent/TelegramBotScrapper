package backend.academy.scrapper.client;

import backend.academy.scrapper.dto.LinkUpdateRequest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class BotApiClient {
    private static final Logger log = LoggerFactory.getLogger(BotApiClient.class);
    private final WebClient webClient;
    private final Retry retrySpec;
    private final CircuitBreaker circuitBreaker;

    public BotApiClient(
            @Qualifier("botWebClient") WebClient webClient,
            @Value("${retry.max-attempts:3}") int maxAttempts,
            @Value("${retry.first-backoff-seconds:1}") long backoffSeconds,
            @Qualifier("circuitBreakerRegistry") CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = webClient;
        this.retrySpec = Retry.fixedDelay(maxAttempts, Duration.ofSeconds(backoffSeconds))
                .filter(throwable -> {
                    if (throwable instanceof WebClientResponseException ex) {
                        return List.of(500, 502, 503, 504)
                                .contains(ex.getStatusCode().value());
                    }
                    return false;
                })
                .doBeforeRetry(signal -> log.debug("Retrying Bot API request, attempt: {}", signal.totalRetries() + 1));
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("botApiClient");
    }

    public Mono<Void> postUpdate(LinkUpdateRequest update) {
        return webClient
                .post()
                .uri("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(update)
                .retrieve()
                .toBodilessEntity()
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .retryWhen(retrySpec)
                .doOnError(error -> log.error("Error posting update: {}", error.getMessage()))
                .onErrorMap(throwable -> {
                    if (throwable instanceof WebClientResponseException ex) {
                        log.error(
                                "API error for update: status code {}, response body: {}",
                                ex.getStatusCode(),
                                ex.getResponseBodyAsString());
                        return new RuntimeException("Bot API error: " + ex.getStatusCode(), ex);
                    }
                    return throwable;
                })
                .then();
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
}
