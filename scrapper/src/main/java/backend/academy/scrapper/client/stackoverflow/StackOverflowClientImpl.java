package backend.academy.scrapper.client.stackoverflow;

import backend.academy.scrapper.client.ResilienceUtils;
import backend.academy.scrapper.dto.AnswerResponse;
import backend.academy.scrapper.dto.AnswersApiResponse;
import backend.academy.scrapper.dto.QuestionResponse;
import backend.academy.scrapper.dto.QuestionsApiResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class StackOverflowClientImpl implements StackOverflowClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackOverflowClientImpl.class);
    private static final String SITE = "site";
    private static final String STACKOVERFLOW = "stackoverflow";
    private static final String API_ERROR = "StackOverflow API error";

    private final ConcurrentHashMap<String, Mono<List<QuestionResponse>>> questionsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Mono<List<AnswerResponse>>> answersCache = new ConcurrentHashMap<>();

    private final WebClient webClient;
    private final ResilienceUtils resilienceUtils;
    private final CircuitBreaker circuitBreaker;

    public StackOverflowClientImpl(
            @Qualifier("stackOverflowWebClient") WebClient webClient,
            ResilienceUtils resilienceUtils,
            @Qualifier("circuitBreakerRegistry") CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = webClient;
        this.resilienceUtils = resilienceUtils;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("stackOverflowClient");
    }

    @Override
    public Mono<List<QuestionResponse>> fetchQuestionsInfo(List<String> questionIds) {
        String ids = String.join(";", questionIds);
        return questionsCache.computeIfAbsent(ids, key -> {
            LOGGER.debug("Fetching questions info for IDs: {}", ids);

            Mono<List<QuestionResponse>> responseMono = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{ids}")
                            .queryParam(SITE, STACKOVERFLOW)
                            .queryParam("filter", "withbody")
                            .build(ids))
                    .retrieve()
                    .bodyToMono(QuestionsApiResponse.class)
                    .map(QuestionsApiResponse::getItems);

            return resilienceUtils
                    .decorateWithRetry(responseMono, LOGGER, "StackOverflow")
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    .publishOn(Schedulers.boundedElastic())
                    .doOnSuccess(questions -> LOGGER.debug("Successfully fetched questions: {}", ids))
                    .doOnError(error -> LOGGER.error("Error fetching questions {}: {}", ids, error.getMessage()))
                    .onErrorResume(throwable -> {
                        if (reactor.core.Exceptions.isRetryExhausted(throwable)
                                || throwable instanceof CallNotPermittedException) {
                            LOGGER.error("Fallback: Failed to fetch questions {}: {}", ids, throwable.getMessage());
                            return Mono.just(List.of());
                        }
                        return Mono.error(throwable);
                    })
                    .cache();
        });
    }

    @Override
    public Mono<List<AnswerResponse>> fetchAnswersInfo(List<String> questionIds) {
        String joinedQuestionIds = String.join(";", questionIds);

        return answersCache.computeIfAbsent(joinedQuestionIds, ids -> {
            LOGGER.debug("Fetching answers info for question IDs: {}", ids);

            Mono<List<AnswerResponse>> responseMono = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{ids}/answers")
                            .queryParam(SITE, STACKOVERFLOW)
                            .build(ids))
                    .retrieve()
                    .bodyToMono(AnswersApiResponse.class)
                    .map(AnswersApiResponse::getItems)
                    .switchIfEmpty(Mono.just(List.of()));

            return resilienceUtils
                    .decorateWithRetry(responseMono, LOGGER, "StackOverflow")
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    .publishOn(Schedulers.boundedElastic())
                    .doOnSuccess(items -> LOGGER.debug("Successfully fetched {} answers for {}", items.size(), ids))
                    .doOnError(error -> LOGGER.error("Error fetching answers for {}: {}", ids, error.getMessage()))
                    .onErrorResume(throwable -> {
                        if (reactor.core.Exceptions.isRetryExhausted(throwable)
                                || throwable instanceof CallNotPermittedException) {
                            LOGGER.error("Fallback: Failed to fetch answers {}: {}", ids, throwable.getMessage());
                            return Mono.just(List.of());
                        }
                        return Mono.error(throwable);
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
