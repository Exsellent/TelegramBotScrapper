package backend.academy.scrapper.client;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

public final class ResilienceUtils {

    private ResilienceUtils() {
        // Утилитный класс, предотвращаем инстанцирование
    }

    public static Retry createRetrySpec(int maxAttempts, long backoffSeconds, Logger logger, String serviceName) {
        return Retry.fixedDelay(maxAttempts, Duration.ofSeconds(backoffSeconds))
                .filter(throwable -> {
                    if (throwable instanceof WebClientResponseException ex) {
                        return List.of(500, 502, 503, 504)
                                .contains(ex.getStatusCode().value());
                    }
                    return false;
                })
                .doBeforeRetry(signal ->
                        logger.debug("Retrying {} request, attempt: {}", serviceName, signal.totalRetries() + 1));
    }
}
