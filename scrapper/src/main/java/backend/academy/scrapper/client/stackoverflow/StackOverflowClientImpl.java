package backend.academy.scrapper.client.stackoverflow;

import backend.academy.scrapper.dto.AnswerResponse;
import backend.academy.scrapper.dto.AnswersApiResponse;
import backend.academy.scrapper.dto.QuestionResponse;
import backend.academy.scrapper.dto.QuestionsApiResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class StackOverflowClientImpl implements StackOverflowClient {

    private static final String SITE = "site";
    private static final String STACKOVERFLOW = "stackoverflow";
    private static final String API_ERROR = "API Error";

    private final WebClient webClient;
    private final Retry retrySpec;

    public StackOverflowClientImpl(@Qualifier("stackOverflowWebClient") WebClient webClient, Retry retrySpec) {
        this.webClient = webClient;
        this.retrySpec = retrySpec;
    }

    @Override
    public Mono<List<QuestionResponse>> fetchQuestionsInfo(List<String> questionIds) {
        String joinedQuestionIds = String.join(";", questionIds);

        Mono<List<QuestionResponse>> mono = webClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/questions/{ids}")
                .queryParam(SITE, STACKOVERFLOW)
                .build(joinedQuestionIds))
            .exchangeToMono(response -> {
                if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                    return Mono.error(new RuntimeException(API_ERROR));
                }
                return response.bodyToMono(QuestionsApiResponse.class)
                    .map(QuestionsApiResponse::getItems);
            });
        return retrySpec != null ? mono.retryWhen(retrySpec) : mono;
    }

    @Override
    public Mono<List<AnswerResponse>> fetchAnswersInfo(List<String> questionIds) {
        String joinedQuestionIds = String.join(";", questionIds);

        Mono<List<AnswerResponse>> mono = webClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/questions/{id}/answers")
                .queryParam(SITE, STACKOVERFLOW)
                .build(joinedQuestionIds))
            .exchangeToMono(response -> {
                if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                    return Mono.error(new RuntimeException(API_ERROR));
                }
                return response.bodyToMono(AnswersApiResponse.class)
                    .map(AnswersApiResponse::getItems);
            });
        return retrySpec != null ? mono.retryWhen(retrySpec) : mono;
    }
}
