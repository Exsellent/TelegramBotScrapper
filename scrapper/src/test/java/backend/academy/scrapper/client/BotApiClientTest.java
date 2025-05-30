package backend.academy.scrapper.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import backend.academy.scrapper.dto.LinkUpdateRequest;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class BotApiClientTest {

    private WireMockServer wireMockServer;
    private BotApiClient botApiClient;

    @BeforeEach
    void setUp() {
        wireMockServer =
                new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .build();

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(2) // Уменьшаем до 2
                .minimumNumberOfCalls(2) // Уменьшаем до 2, чтобы CircuitBreaker сработал быстрее
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        ResilienceUtils resilienceUtils = new ResilienceUtils(2, 1L, Arrays.asList(500, 502, 503, 504, 429));

        botApiClient = new BotApiClient(webClient, resilienceUtils, circuitBreakerRegistry);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testCircuitBreakerAndFallback() {
        wireMockServer.stubFor(
                post(urlEqualTo("/updates")).willReturn(aResponse().withStatus(503)));

        LinkUpdateRequest request = new LinkUpdateRequest();

        // Первая попытка: 2 ретрая, затем fallback
        StepVerifier.create(botApiClient.postUpdate(request)).verifyComplete();

        // Вторая попытка: CircuitBreaker должен сработать (2 вызова, 50% failure rate)
        StepVerifier.create(botApiClient.postUpdate(request)).verifyComplete();

        // Третья попытка: CircuitBreaker уже открыт
        StepVerifier.create(botApiClient.postUpdate(request)).verifyComplete();

        // Проверяем, что CircuitBreaker открыт
        assert botApiClient.getCircuitBreaker().getState()
                == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
    }
}
