package backend.academy.scrapper.client;

import backend.academy.scrapper.dto.LinkUpdateRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
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
        this.retrySpec = ResilienceUtils.createRetrySpec(maxAttempts, backoffSeconds, log, "Bot API");
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
                .onErrorResume(throwable -> {
                    if (throwable instanceof CallNotPermittedException
                            || reactor.core.Exceptions.isRetryExhausted(throwable)) {
                        log.error("Fallback: Failed to post update: {}", throwable.getMessage());
                        return Mono.empty();
                    }
                    if (throwable instanceof WebClientResponseException ex) {
                        log.error(
                                "API error for update: status code {}, response body: {}",
                                ex.getStatusCode(),
                                ex.getResponseBodyAsString());
                    }
                    return Mono.error(throwable);
                })
                .then();
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
}
