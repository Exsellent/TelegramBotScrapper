package backend.academy.scrapper.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.scrapper.dto.AnswerResponse;
import backend.academy.scrapper.dto.CombinedStackOverflowInfo;
import backend.academy.scrapper.dto.QuestionResponse;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class StackOverflowServiceTest {

    @Mock
    private StackOverflowClient stackOverflowClient;

    @InjectMocks
    private StackOverflowService stackOverflowService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        QuestionResponse mockQuestionResponse =
                new QuestionResponse(1L, "Test Question", OffsetDateTime.now(), OffsetDateTime.now());
        AnswerResponse mockAnswerResponse1 = new AnswerResponse(
                2L, OffsetDateTime.now().minusDays(1), OffsetDateTime.now().minusDays(1), 1L);
        AnswerResponse mockAnswerResponse2 = new AnswerResponse(3L, OffsetDateTime.now(), OffsetDateTime.now(), 1L);
        List<AnswerResponse> mockAnswers = List.of(mockAnswerResponse1, mockAnswerResponse2);

        when(stackOverflowClient.fetchQuestionsInfo(anyList()))
                .thenReturn(Mono.just(Collections.singletonList(mockQuestionResponse)));
        when(stackOverflowClient.fetchAnswersInfo(anyList())).thenReturn(Mono.just(mockAnswers));
    }

    @Test
    public void getQuestionInfoTest() {
        Mono<QuestionResponse> result = stackOverflowService.getQuestionInfo("1");

        StepVerifier.create(result)
                .expectNextMatches(question ->
                        question.getQuestionId() == 1L && question.getTitle().equals("Test Question"))
                .verifyComplete();
    }

    @Test
    public void getAnswersForQuestionTest() {
        Mono<List<AnswerResponse>> result = stackOverflowService.getAnswersForQuestion("2");

        StepVerifier.create(result)
                .expectNextMatches(
                        answers -> answers.size() == 2 && answers.get(0).getAnswerId() == 2L)
                .verifyComplete();
    }

    @Test
    public void getAllQuestionsInfoTest() {
        Mono<List<QuestionResponse>> result = stackOverflowService.getAllQuestionsInfo(Arrays.asList("1", "4"));

        StepVerifier.create(result)
                .expectNextMatches(questions ->
                        questions.size() == 1 && questions.get(0).getTitle().equals("Test Question"))
                .verifyComplete();
    }

    @Test
    public void getAllAnswersInfoTest() {
        Mono<List<AnswerResponse>> result = stackOverflowService.getAllAnswersInfo(Arrays.asList("1", "4"));

        StepVerifier.create(result)
                .expectNextMatches(
                        answers -> answers.size() == 2 && answers.get(0).getAnswerId() == 2L)
                .verifyComplete();
    }

    @Test
    public void getCombinedInfoTest() {
        String questionId = "1";
        Mono<CombinedStackOverflowInfo> result = stackOverflowService.getCombinedInfo(questionId);

        StepVerifier.create(result)
                .expectNextMatches(combinedInfo -> {
                    QuestionResponse question = combinedInfo.getQuestion();
                    List<AnswerResponse> answers = combinedInfo.getAnswers();

                    return question.getQuestionId().equals(1L)
                            && question.getTitle().equals("Test Question")
                            && answers.size() == 2
                            && answers.stream()
                                    .anyMatch(answer -> answer.getAnswerId().equals(2L))
                            && answers.stream()
                                    .anyMatch(answer -> answer.getAnswerId().equals(3L));
                })
                .verifyComplete();
    }
}
