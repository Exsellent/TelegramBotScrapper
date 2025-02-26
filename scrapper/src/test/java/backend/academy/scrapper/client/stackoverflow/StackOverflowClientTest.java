package backend.academy.scrapper.client.stackoverflow;

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
import org.springframework.core.codec.DecodingException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@ExtendWith(MockitoExtension.class)
public class StackOverflowClientTest {

    private WireMockServer wireMockServer;
    private StackOverflowClient stackOverflowClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().port(8089));
        wireMockServer.start();
        configureFor("localhost", 8089);

        WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:8089")
            .build();
        stackOverflowClient = new StackOverflowClientImpl(webClient, null); // Убираем Retry для упрощения

        setUpQuestionsInfo();
        setUpAnswersInfo();
    }

    void setUpQuestionsInfo() {
        wireMockServer.stubFor(get(urlPathMatching("/questions/.*"))
            .withQueryParam("site", equalTo("stackoverflow"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody("{\"items\": [{\"question_id\": 123, \"title\": \"Test Question 1\", \"last_activity_date\": 1577836800}, {\"question_id\": 456, \"title\": \"Test Question 2\", \"last_activity_date\": 1577836800}]}")));
    }

    void setUpAnswersInfo() {
        wireMockServer.stubFor(get(urlPathEqualTo("/questions/123/answers"))
            .withQueryParam("site", equalTo("stackoverflow"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody("{\"items\": [{\"answer_id\": 789, \"question_id\": 123, \"last_activity_date\": 1577836800}]}")));
    }

    @Test
    void fetchQuestionsInfoTest() {
        List<String> questionIds = Arrays.asList("123", "456");
        Mono<List<QuestionResponse>> response = stackOverflowClient.fetchQuestionsInfo(questionIds);
        StepVerifier.create(response)
            .expectNextMatches(questions -> questions.size() == 2 && questions.get(0).getTitle().equals("Test Question 1"))
            .verifyComplete();
    }

    @Test
    void fetchAnswersInfoTest() {
        List<String> questionIds = Collections.singletonList("123");
        Mono<List<AnswerResponse>> response = stackOverflowClient.fetchAnswersInfo(questionIds);
        StepVerifier.create(response)
            .expectNextMatches(answers -> answers.size() == 1 && answers.get(0).getAnswerId() == 789 && answers.get(0).getQuestionId() == 123)
            .verifyComplete();
    }

    @Test
    void fetchQuestionsInfo_NotFound() {
        wireMockServer.stubFor(get(urlPathMatching("/questions/.*"))
            .withQueryParam("site", equalTo("stackoverflow"))
            .willReturn(aResponse().withStatus(404)));

        Mono<List<QuestionResponse>> response = stackOverflowClient.fetchQuestionsInfo(Arrays.asList("123"));

        StepVerifier.create(response)
            .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                && throwable.getMessage().equals("API Error"))
            .verify();
    }

    @Test
    void fetchAnswersInfo_InternalServerError() {
        wireMockServer.stubFor(get(urlPathEqualTo("/questions/123/answers"))
            .withQueryParam("site", equalTo("stackoverflow"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")));

        Mono<List<AnswerResponse>> response = stackOverflowClient.fetchAnswersInfo(Collections.singletonList("123"));

        StepVerifier.create(response)
            .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                && throwable.getMessage().equals("API Error"))
            .verify();
    }

    @Test
    void fetchQuestionsInfo_InvalidJsonResponse() {
        wireMockServer.stubFor(get(urlPathMatching("/questions/.*"))
            .withQueryParam("site", equalTo("stackoverflow"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody("{invalid_json}")));

        Mono<List<QuestionResponse>> response = stackOverflowClient.fetchQuestionsInfo(Arrays.asList("123"));

        StepVerifier.create(response)
            .expectErrorMatches(throwable -> throwable instanceof DecodingException)
            .verify();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }
}
