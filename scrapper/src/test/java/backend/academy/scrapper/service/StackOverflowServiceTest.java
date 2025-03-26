package backend.academy.scrapper.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.scrapper.dto.AnswerResponse;
import backend.academy.scrapper.dto.CombinedStackOverflowInfo;
import backend.academy.scrapper.dto.QuestionResponse;
import backend.academy.scrapper.dto.User;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class StackOverflowServiceTest {

    @Mock
    private StackOverflowClient stackOverflowClient;

    @InjectMocks
    private StackOverflowService stackOverflowService;

    private QuestionResponse mockQuestionResponse;
    private List<AnswerResponse> mockAnswers;

    @BeforeEach
    public void setup() {
        OffsetDateTime now = OffsetDateTime.now();

        mockQuestionResponse = new QuestionResponse(
                1L, "Test Question", now, now, new User(null, "testUser", null, null), "Question body");

        AnswerResponse mockAnswerResponse1 = new AnswerResponse(
                2L, now.minusDays(1), now.minusDays(1), 1L, new User(null, "testUser1", null, null), "Answer body 1");
        AnswerResponse mockAnswerResponse2 =
                new AnswerResponse(3L, now, now, 1L, new User(null, "testUser2", null, null), "Answer body 2");
        mockAnswers = List.of(mockAnswerResponse1, mockAnswerResponse2);
    }

    @Test
    public void getQuestionInfoTest() {
        when(stackOverflowClient.fetchQuestionsInfo(anyList()))
                .thenReturn(Mono.just(Collections.singletonList(mockQuestionResponse)));

        Mono<QuestionResponse> result = stackOverflowService.getQuestionInfo("1");

        StepVerifier.create(result)
                .expectNextMatches(question ->
                        question.getQuestionId() == 1L && question.getTitle().equals("Test Question"))
                .verifyComplete();
    }

    @Test
    public void getAnswersForQuestionTest() {
        when(stackOverflowClient.fetchAnswersInfo(anyList())).thenReturn(Mono.just(mockAnswers));

        Mono<List<AnswerResponse>> result = stackOverflowService.getAnswersForQuestion("2");

        StepVerifier.create(result)
                .expectNextMatches(
                        answers -> answers.size() == 2 && answers.get(0).getAnswerId() == 2L)
                .verifyComplete();
    }

    @Test
    public void getAllQuestionsInfoTest() {
        when(stackOverflowClient.fetchQuestionsInfo(anyList()))
                .thenReturn(Mono.just(Collections.singletonList(mockQuestionResponse)));

        Mono<List<QuestionResponse>> result = stackOverflowService.getAllQuestionsInfo(Arrays.asList("1", "4"));

        StepVerifier.create(result)
                .expectNextMatches(questions ->
                        questions.size() == 1 && questions.get(0).getTitle().equals("Test Question"))
                .verifyComplete();
    }

    @Test
    public void getAllAnswersInfoTest() {
        when(stackOverflowClient.fetchAnswersInfo(anyList())).thenReturn(Mono.just(mockAnswers));

        Mono<List<AnswerResponse>> result = stackOverflowService.getAllAnswersInfo(Arrays.asList("1", "4"));

        StepVerifier.create(result)
                .expectNextMatches(
                        answers -> answers.size() == 2 && answers.get(0).getAnswerId() == 2L)
                .verifyComplete();
    }

    @Test
    public void getCombinedInfoTest() {
        when(stackOverflowClient.fetchQuestionsInfo(anyList()))
                .thenReturn(Mono.just(Collections.singletonList(mockQuestionResponse)));
        when(stackOverflowClient.fetchAnswersInfo(anyList())).thenReturn(Mono.just(mockAnswers));

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
