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
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        botApiClient = new BotApiClient(webClient, 2, 1L, circuitBreakerRegistry);
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

        // Первая попытка: срабатывает 2 ретрая, потом fallback (Mono.empty)
        StepVerifier.create(botApiClient.postUpdate(request)).verifyComplete(); // fallback -> Mono.empty()

        // Вызов еще раз, чтобы добиться открытия CircuitBreaker
        StepVerifier.create(botApiClient.postUpdate(request)).verifyComplete(); // fallback снова

        // После нескольких неудач CircuitBreaker должен быть открыт
        StepVerifier.create(botApiClient.postUpdate(request))
                .verifyComplete(); // запрос даже не пойдет, т.к. CircuitBreaker открыт

        // Проверить что CircuitBreaker действительно открылся
        assert botApiClient.getCircuitBreaker().getState()
                == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
    }
}
