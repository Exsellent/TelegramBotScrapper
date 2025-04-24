package backend.academy.scrapper.client.stackoverflow;

import backend.academy.scrapper.dto.AnswerResponse;
import backend.academy.scrapper.dto.AnswersApiResponse;
import backend.academy.scrapper.dto.QuestionResponse;
import backend.academy.scrapper.dto.QuestionsApiResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Service
public class StackOverflowClientImpl implements StackOverflowClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackOverflowClientImpl.class);
    private static final String SITE = "site";
    private static final String STACKOVERFLOW = "stackoverflow";
    private static final String API_ERROR = "StackOverflow API error";

    private final ConcurrentHashMap<String, Mono<List<QuestionResponse>>> questionsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Mono<List<AnswerResponse>>> answersCache = new ConcurrentHashMap<>();

    private final WebClient webClient;
    private final Retry retrySpec;
    private final CircuitBreaker circuitBreaker;

    public StackOverflowClientImpl(
            @Qualifier("stackOverflowWebClient") WebClient webClient,
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
                .doBeforeRetry(signal ->
                        LOGGER.debug("Retrying StackOverflow request, attempt: {}", signal.totalRetries() + 1));
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("stackOverflowClient");
    }

    @Override
    public Mono<List<QuestionResponse>> fetchQuestionsInfo(List<String> questionIds) {
        String ids = String.join(";", questionIds);
        return questionsCache.computeIfAbsent(ids, key -> {
            LOGGER.debug("Fetching questions info for IDs: {}", ids);

            return webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{ids}")
                            .queryParam(SITE, STACKOVERFLOW)
                            .queryParam("filter", "withbody")
                            .build(ids))
                    .retrieve()
                    .bodyToMono(QuestionsApiResponse.class)
                    .map(QuestionsApiResponse::getItems)
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    .retryWhen(retrySpec)
                    .publishOn(Schedulers.boundedElastic())
                    .doOnSuccess(questions -> LOGGER.debug("Successfully fetched questions: {}", ids))
                    .doOnError(error -> LOGGER.error("Error fetching questions {}: {}", ids, error.getMessage()))
                    .onErrorMap(throwable -> {
                        if (throwable instanceof WebClientResponseException ex) {
                            LOGGER.error(
                                    "API error for questions {}: status code {}, response body: {}",
                                    ids,
                                    ex.getStatusCode(),
                                    ex.getResponseBodyAsString());
                            return new RuntimeException(API_ERROR + ": " + ex.getStatusCode(), ex);
                        }
                        return throwable;
                    })
                    .cache();
        });
    }

    @Override
    public Mono<List<AnswerResponse>> fetchAnswersInfo(List<String> questionIds) {
        String joinedQuestionIds = String.join(";", questionIds);

        return answersCache.computeIfAbsent(joinedQuestionIds, ids -> {
            LOGGER.debug("Fetching answers info for question IDs: {}", ids);

            return webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{ids}/answers")
                            .queryParam(SITE, STACKOVERFLOW)
                            .build(ids))
                    .retrieve()
                    .bodyToMono(AnswersApiResponse.class)
                    .map(AnswersApiResponse::getItems)
                    .switchIfEmpty(Mono.just(List.of()))
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    .retryWhen(retrySpec)
                    .publishOn(Schedulers.boundedElastic())
                    .doOnSuccess(items -> LOGGER.debug("Successfully fetched {} answers for {}", items.size(), ids))
                    .doOnError(error -> LOGGER.error("Error fetching answers for {}: {}", ids, error.getMessage()))
                    .onErrorMap(throwable -> {
                        if (throwable instanceof WebClientResponseException ex) {
                            LOGGER.error(
                                    "API error for answers {}: status code {}, response body: {}",
                                    ids,
                                    ex.getStatusCode(),
                                    ex.getResponseBodyAsString());
                            return new RuntimeException(API_ERROR + ": " + ex.getStatusCode(), ex);
                        }
                        return throwable;
                    })
                    .cache();
        });
    }

    public void clearCaches() {
        LOGGER.debug("Clearing StackOverflow client caches");
        questionsCache.clear();
        answersCache.clear();
    }
}
