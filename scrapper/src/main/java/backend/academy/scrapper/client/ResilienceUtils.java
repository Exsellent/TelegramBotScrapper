package backend.academy.scrapper.client;

import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@Component
public class ResilienceUtils {

    private final int maxAttempts;
    private final long backoffSeconds;
    private final List<Integer> retryableStatusCodes;

    @Autowired
    public ResilienceUtils(
            @Value("${retry.max-attempts:5}") int maxAttempts,
            @Value("${retry.first-backoff-seconds:5}") long backoffSeconds,
            @Value("${retry.retryable-status-codes}") String retryableCodesRaw) {
        this.maxAttempts = maxAttempts;
        this.backoffSeconds = backoffSeconds;
        this.retryableStatusCodes = Arrays.stream(retryableCodesRaw.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    // Второй конструктор —  использование в тестах
    public ResilienceUtils(int maxAttempts, long backoffSeconds, List<Integer> retryableStatusCodes) {
        this.maxAttempts = maxAttempts;
        this.backoffSeconds = backoffSeconds;
        this.retryableStatusCodes = retryableStatusCodes;
    }

    /**
     * Декорирует Mono операцию retry логикой.
     *
     * @param mono исходный Mono
     * @param logger логгер для отслеживания retry попыток
     * @param serviceName имя сервиса для логирования
     * @return Mono с примененной retry логикой
     */
    public <T> Mono<T> decorateWithRetry(Mono<T> mono, Logger logger, String serviceName) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofSeconds(backoffSeconds))
                .retryOnException(throwable -> {
                    if (throwable instanceof WebClientResponseException ex) {
                        boolean shouldRetry =
                                retryableStatusCodes.contains(ex.getStatusCode().value());
                        if (!shouldRetry) {
                            logger.debug(
                                    "Not retrying {} request for status code: {}",
                                    serviceName,
                                    ex.getStatusCode().value());
                        }
                        return shouldRetry;
                    }
                    return false;
                })
                .build();

        Retry retry = Retry.of(serviceName, config);

        retry.getEventPublisher()
                .onRetry(event -> logger.debug(
                        "Retrying {} request, attempt: {} of {}",
                        serviceName,
                        event.getNumberOfRetryAttempts(),
                        maxAttempts));

        return mono.transformDeferred(RetryOperator.of(retry)).onErrorResume(throwable -> {
            if (Exceptions.isRetryExhausted(throwable)) {
                logger.debug("Retry exhausted for {} request", serviceName);
                return Mono.empty(); // fallback
            }
            return Mono.error(throwable);
        });
    }
}
