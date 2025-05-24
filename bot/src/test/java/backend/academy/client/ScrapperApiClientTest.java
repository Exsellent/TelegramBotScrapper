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
import backend.academy.bot.exception.ApiException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

public class ScrapperApiClientTest {
    private WireMockServer wireMockServer;
    private ScrapperApiClient scrapperApiClient;

    @BeforeEach
    void setUp() {
        wireMockServer =
                new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        // Настраиваем SimpleClientHttpRequestFactory
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);

        // Создаем RestClient
        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, (request, response) -> {
                    if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        throw new ApiException("Rate limit exceeded: " + response.getStatusCode());
                    }
                    throw new ApiException("Client error: " + response.getStatusCode());
                })
                .defaultStatusHandler(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ApiException("Server error: " + response.getStatusCode());
                })
                .build();

        // Создаем RetryTemplate
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)
                .fixedBackoff(1000)
                .retryOn(ApiException.class)
                .build();

        // Настраиваем CircuitBreakerRegistry
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .permittedNumberOfCallsInHalfOpenState(3)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .recordExceptions(ApiException.class)
                .build();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        // Создаем ScrapperApiClient с новым конструктором
        scrapperApiClient = new ScrapperApiClient(
                restClient, "http://localhost:" + wireMockServer.port(), retryTemplate, circuitBreakerRegistry);
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
                    ApiException.class,
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
