package backend.academy.scrapper.client.stackoverflow;

import backend.academy.scrapper.dto.AnswerResponse;
import backend.academy.scrapper.dto.QuestionResponse;
import java.util.List;
import reactor.core.publisher.Mono;

public interface StackOverflowClient {
    Mono<List<QuestionResponse>> fetchQuestionsInfo(List<String> questionIds);

    Mono<List<AnswerResponse>> fetchAnswersInfo(List<String> questionIds);
}
