package backend.academy.scrapper.client.stackoverflow;

import backend.academy.scrapper.dto.AnswerResponse;
import backend.academy.scrapper.dto.AnswersApiResponse;
import backend.academy.scrapper.dto.QuestionResponse;
import backend.academy.scrapper.dto.QuestionsApiResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

/**
 * Клиент для работы с API StackOverflow.
 * Предоставляет методы для получения информации о вопросах и ответах.
 */
@Service
public class StackOverflowClientImpl implements StackOverflowClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackOverflowClientImpl.class);
    private static final String SITE = "site";
    private static final String STACKOVERFLOW = "stackoverflow";
    private static final String API_ERROR = "StackOverflow API error";

    // Кэши для предотвращения дублирования вызовов API
    private final ConcurrentHashMap<String, Mono<List<QuestionResponse>>> questionsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Mono<List<AnswerResponse>>> answersCache = new ConcurrentHashMap<>();

    private final WebClient webClient;
    private final Retry retrySpec;

    public StackOverflowClientImpl(@Qualifier("stackOverflowWebClient") WebClient webClient, Retry retrySpec) {
        this.webClient = webClient;
        // Настройка retry по умолчанию, если не передан извне
        this.retrySpec = retrySpec != null ? retrySpec : Retry.backoff(3, Duration.ofSeconds(2))
            .filter(throwable -> throwable instanceof RuntimeException)
            .doBeforeRetry(retrySignal -> LOGGER.warn("Retrying after error: {}", retrySignal.failure().getMessage()));
    }

    @Override
    public Mono<List<QuestionResponse>> fetchQuestionsInfo(List<String> questionIds) {
        String joinedQuestionIds = String.join(";", questionIds);

        return questionsCache.computeIfAbsent(joinedQuestionIds, ids -> {
            LOGGER.debug("Fetching questions info for IDs: {}", ids);

            return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/questions/{ids}")
                    .queryParam(SITE, STACKOVERFLOW)
                    .build(ids))
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                        LOGGER.error("API error for questions {}: status code {}", ids, response.statusCode());
                        return Mono.error(new RuntimeException(API_ERROR + ": " + response.statusCode()));
                    }
                    return response.bodyToMono(QuestionsApiResponse.class)
                        .map(QuestionsApiResponse::getItems)
                        .switchIfEmpty(Mono.just(List.of()));
                })
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(items -> LOGGER.debug("Successfully fetched {} questions", items.size()))
                .doOnError(error -> LOGGER.error("Error fetching questions for {}: {}", ids, error.getMessage()))
                .retryWhen(retrySpec)
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
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                        LOGGER.error("API error for answers {}: status code {}", ids, response.statusCode());
                        return Mono.error(new RuntimeException(API_ERROR + ": " + response.statusCode()));
                    }
                    return response.bodyToMono(AnswersApiResponse.class)
                        .map(AnswersApiResponse::getItems)
                        .switchIfEmpty(Mono.just(List.of()));
                })
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(items -> LOGGER.debug("Successfully fetched {} answers", items.size()))
                .doOnError(error -> LOGGER.error("Error fetching answers for {}: {}", ids, error.getMessage()))
                .retryWhen(retrySpec)
                .cache();
        });
    }

    /**
     * Очищает кэши для предотвращения утечек памяти.
     * Следует вызывать периодически или после завершения обработки.
     */
    public void clearCaches() {
        LOGGER.debug("Clearing StackOverflow client caches");
        questionsCache.clear();
        answersCache.clear();
    }
}
