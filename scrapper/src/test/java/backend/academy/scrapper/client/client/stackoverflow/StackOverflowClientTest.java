package backend.academy.scrapper.client.client.stackoverflow;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import backend.academy.scrapper.client.stackoverflow.StackOverflowClientImpl;
import backend.academy.scrapper.dto.QuestionResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
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
    private StackOverflowClientImpl stackOverflowClient;

    @BeforeEach
    void setUp() {
        wireMockServer =
                new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .build();

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .permittedNumberOfCallsInHalfOpenState(3)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .build();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        stackOverflowClient = new StackOverflowClientImpl(webClient, 3, 1L, circuitBreakerRegistry);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
        wireMockServer.stop();
    }

    @Test
    void testFetchQuestionsInfoSuccess() {
        wireMockServer.stubFor(
                get(urlMatching("/questions/123.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"items\": [{\"question_id\": 123, \"title\": \"Test Question\", \"last_activity_date\": 1577836800}]}")));

        Mono<List<QuestionResponse>> response = stackOverflowClient.fetchQuestionsInfo(List.of("123"));

        StepVerifier.create(response)
                .expectNextMatches(questions ->
                        questions.size() == 1 && questions.get(0).getTitle().equals("Test Question"))
                .verifyComplete();

        wireMockServer.verify(1, getRequestedFor(urlMatching("/questions/123.*")));
    }

    @Test
    void testFetchQuestionsInfoCircuitBreaker() {
        wireMockServer.resetAll();
        stackOverflowClient.clearCaches(); // Очищаем кэш перед тестом

        wireMockServer.stubFor(get(urlMatching("/questions/123.*"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Service Unavailable\"}")));

        Mono<List<QuestionResponse>> response = stackOverflowClient.fetchQuestionsInfo(List.of("123"));

        StepVerifier.create(response)
                .expectNextMatches(questions -> questions.isEmpty()) // Ожидаем пустой список из fallback
                .verifyComplete();

        wireMockServer
                .findAll(getRequestedFor(urlMatching("/questions/123.*")))
                .forEach(req -> System.out.println("Request: " + req));
        wireMockServer.verify(exactly(4), getRequestedFor(urlMatching("/questions/123.*"))); // Исправлено на 4
    }
}
