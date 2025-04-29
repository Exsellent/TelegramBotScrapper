package backend.academy.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.bot.client.ScrapperApiClient;
import backend.academy.bot.dto.AddLinkRequest;
import backend.academy.bot.dto.ListLinksResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

public class ScrapperApiClientTest {
    private WireMockServer wireMockServer;
    private ScrapperApiClient scrapperApiClient;

    @BeforeEach
    void setUp() {
        wireMockServer =
                new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .permittedNumberOfCallsInHalfOpenState(3)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .recordExceptions(backend.academy.bot.exception.ApiException.class)
                .build();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        scrapperApiClient = new ScrapperApiClient(
                "http://localhost:" + wireMockServer.port(), requestFactory, 3, 1, circuitBreakerRegistry);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
        wireMockServer.stop();
    }

    @Test
    void testGetAllLinksSuccess() {
        wireMockServer.stubFor(get(urlEqualTo("/chats/123/links"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"links\": []}")));

        ListLinksResponse response = scrapperApiClient.getAllLinks(123L);

        assertNotNull(response);
        assertTrue(response.getLinks().isEmpty());
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/chats/123/links")));
    }

    @Test
    void testGetAllLinksRetry() {
        wireMockServer.stubFor(get(urlEqualTo("/chats/123/links"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Service Unavailable\"}"))
                .willSetStateTo("Retry Attempt 1"));

        wireMockServer.stubFor(get(urlEqualTo("/chats/123/links"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Retry Attempt 1")
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Service Unavailable\"}"))
                .willSetStateTo("Retry Attempt 2"));

        wireMockServer.stubFor(get(urlEqualTo("/chats/123/links"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Retry Attempt 2")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"links\": []}")));

        ListLinksResponse response = scrapperApiClient.getAllLinks(123L);

        assertNotNull(response);
        assertTrue(response.getLinks().isEmpty());
        wireMockServer.verify(3, getRequestedFor(urlEqualTo("/chats/123/links")));
    }

    @Test
    void testGetAllLinksCircuitBreaker() {
        wireMockServer.resetAll();

        // Эмулируем 15 последовательных ошибок 503 для покрытия всех попыток
        for (int i = 0; i < 15; i++) {
            wireMockServer.stubFor(get(urlEqualTo("/chats/123/links"))
                    .inScenario("CircuitBreaker Scenario")
                    .whenScenarioStateIs(i == 0 ? "Started" : "Attempt " + (i - 1))
                    .willReturn(aResponse()
                            .withStatus(503)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"Service Unavailable\"}"))
                    .willSetStateTo("Attempt " + i));
        }

        // Выполняем 5 вызовов, ожидаем ApiException
        for (int i = 0; i < 5; i++) {
            assertThrows(
                    backend.academy.bot.exception.ApiException.class,
                    () -> {
                        scrapperApiClient.getAllLinks(123L);
                    },
                    "Expected ApiException for call " + (i + 1));
        }

        // Шестой вызов должен выбросить CallNotPermittedException
        assertThrows(
                io.github.resilience4j.circuitbreaker.CallNotPermittedException.class,
                () -> {
                    scrapperApiClient.getAllLinks(123L);
                },
                "Expected CallNotPermittedException for call 6");

        // Проверяем состояние Circuit Breaker
        CircuitBreaker circuitBreaker = scrapperApiClient.getCircuitBreaker();
        System.out.println("Circuit Breaker State: " + circuitBreaker.getState());
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // Проверяем количество запросов (ровно 15: 5 вызовов * 3 попытки Retry)
        int requestCount = wireMockServer
                .countRequestsMatching(
                        getRequestedFor(urlEqualTo("/chats/123/links")).build())
                .getCount();
        System.out.println("Number of requests: " + requestCount);
        wireMockServer.verify(exactly(15), getRequestedFor(urlEqualTo("/chats/123/links")));
    }

    @Test
    void testAddLinkSuccess() {
        wireMockServer.stubFor(post(urlEqualTo("/chats/123/links"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")));

        AddLinkRequest request = new AddLinkRequest("https://example.com", null, null);
        assertDoesNotThrow(() -> scrapperApiClient.addLink(123L, request));

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/chats/123/links")));
    }
}
