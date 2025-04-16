package backend.academy.scrapper.client.stackoverflow;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import backend.academy.scrapper.dto.AnswerResponse;
import backend.academy.scrapper.dto.QuestionResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class StackOverflowClientTest {

    private WireMockServer wireMockServer;
    private StackOverflowClient stackOverflowClient;
    private WebClient webClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        webClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .build();
        stackOverflowClient = new StackOverflowClientImpl(webClient, null); // Убираем Retry для упрощения
    }

    @AfterEach
    void tearDown() {
        if (stackOverflowClient instanceof StackOverflowClientImpl) {
            ((StackOverflowClientImpl) stackOverflowClient).clearCaches();
        }
        wireMockServer.resetAll();
        wireMockServer.stop();
    }

    @Test
    void fetchQuestionsInfoTest() {
        wireMockServer.stubFor(
                get(urlPathMatching("/questions/.*"))
                        .withQueryParam("site", equalTo("stackoverflow"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(
                                                "{\"items\": [{\"question_id\": 123, \"title\": \"Test Question 1\", \"last_activity_date\": 1577836800}, {\"question_id\": 456, \"title\": \"Test Question 2\", \"last_activity_date\": 1577836800}]}")));

        List<String> questionIds = Arrays.asList("123", "456");
        Mono<List<QuestionResponse>> response = stackOverflowClient.fetchQuestionsInfo(questionIds);
        StepVerifier.create(response)
                .expectNextMatches(questions ->
                        questions.size() == 2 && questions.get(0).getTitle().equals("Test Question 1"))
                .verifyComplete();
    }

    @Test
    void fetchAnswersInfoTest() {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/questions/123/answers"))
                        .withQueryParam("site", equalTo("stackoverflow"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(
                                                "{\"items\": [{\"answer_id\": 789, \"question_id\": 123, \"last_activity_date\": 1577836800}]}")));

        List<String> questionIds = Collections.singletonList("123");
        Mono<List<AnswerResponse>> response = stackOverflowClient.fetchAnswersInfo(questionIds);
        StepVerifier.create(response)
                .expectNextMatches(answers -> answers.size() == 1
                        && answers.get(0).getAnswerId() == 789
                        && answers.get(0).getQuestionId() == 123)
                .verifyComplete();
    }
}
