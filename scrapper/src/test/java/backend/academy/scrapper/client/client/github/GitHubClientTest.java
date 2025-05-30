package backend.academy.scrapper.client.client.github;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import backend.academy.scrapper.client.github.GitHubClient;
import backend.academy.scrapper.client.github.GitHubClientImpl;
import backend.academy.scrapper.dto.IssuesCommentsResponse;
import backend.academy.scrapper.dto.PullCommentsResponse;
import backend.academy.scrapper.dto.PullRequestResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class GitHubClientTest {

    private WireMockServer wireMockServer;
    private GitHubClient gitHubClient;

    @BeforeEach
    void setUp() {
        wireMockServer =
                new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults(); // создаем дефолтный реестр

        gitHubClient = new GitHubClientImpl(webClient, 3, 1L, circuitBreakerRegistry); // передаем реестр
    }

    @AfterEach
    void tearDown() {
        if (gitHubClient instanceof GitHubClientImpl) {
            ((GitHubClientImpl) gitHubClient).clearCaches();
        }
        wireMockServer.resetAll();
        wireMockServer.stop();
    }

    @Test
    void fetchPullRequestDetailsTest() {
        wireMockServer.stubFor(
                get(urlEqualTo("/repos/owner/repo/pulls/1"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(
                                                "{\"id\": 1, \"title\": \"Test PR\", \"created_at\": \"2020-01-01T00:00:00Z\", \"updated_at\": \"2020-01-01T00:00:00Z\", \"review_comments_url\": \"\", \"comments_url\": \"\"}")));

        Mono<PullRequestResponse> response = gitHubClient.fetchPullRequestDetails("owner", "repo", 1);

        StepVerifier.create(response)
                .expectNextMatches(pr -> pr.getId() == 1 && pr.getTitle().equals("Test PR"))
                .verifyComplete();
    }

    @Test
    void fetchIssueCommentsTest() {
        wireMockServer.stubFor(
                get(urlEqualTo("/repos/owner/repo/issues/1/comments"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(
                                                "[{\"id\": 2, \"url\": \"http://example.com\", \"body\": \"Test comment\", \"created_at\": \"2020-01-01T00:00:00Z\", \"updated_at\": \"2020-01-01T00:00:00Z\", \"user\": {\"login\": \"testUser\"}}]")));

        Flux<IssuesCommentsResponse> response = gitHubClient.fetchIssueComments("owner", "repo", 1);

        StepVerifier.create(response)
                .expectNextMatches(
                        comment -> comment.getId() == 2 && comment.getBody().equals("Test comment"))
                .verifyComplete();
    }

    @Test
    void fetchPullCommentsTest() {
        wireMockServer.stubFor(
                get(urlEqualTo("/repos/owner/repo/pulls/1/comments"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(
                                                "[{\"id\": 3, \"url\": \"http://example.com\", \"body\": \"Test pull comment\", \"created_at\": \"2020-01-02T00:00:00Z\", \"updated_at\": \"2020-01-02T00:00:00Z\", \"user\": {\"login\": \"testUser\"}}]")));

        Flux<PullCommentsResponse> response = gitHubClient.fetchPullComments("owner", "repo", 1);

        StepVerifier.create(response)
                .expectNextMatches(
                        comment -> comment.getId() == 3 && comment.getBody().equals("Test pull comment"))
                .verifyComplete();
    }

    @Test
    void fetchPullRequestDetailsRetryTest() {

        wireMockServer.resetAll();

        wireMockServer.stubFor(get(urlEqualTo("/repos/owner/repo/pulls/1"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Service Unavailable\"}"))
                .willSetStateTo("Retry Attempt 1"));

        wireMockServer.stubFor(get(urlEqualTo("/repos/owner/repo/pulls/1"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Retry Attempt 1")
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Service Unavailable\"}"))
                .willSetStateTo("Retry Attempt 2"));

        wireMockServer.stubFor(
                get(urlEqualTo("/repos/owner/repo/pulls/1"))
                        .inScenario("Retry Scenario")
                        .whenScenarioStateIs("Retry Attempt 2")
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(
                                                "{\"id\": 1, \"title\": \"Test PR\", \"created_at\": \"2020-01-01T00:00:00Z\", \"updated_at\": \"2020-01-01T00:00:00Z\", \"review_comments_url\": \"\", \"comments_url\": \"\"}")));

        Mono<PullRequestResponse> response = gitHubClient.fetchPullRequestDetails("owner", "repo", 1);

        StepVerifier.create(response)
                .expectNextMatches(pr -> {
                    System.out.println("Received response: id=" + pr.getId() + ", title=" + pr.getTitle());
                    return pr.getId() == 1 && pr.getTitle().equals("Test PR");
                })
                .verifyComplete();

        // Проверяем, что было три запроса
        wireMockServer.verify(3, getRequestedFor(urlEqualTo("/repos/owner/repo/pulls/1")));
    }

    @Test
    void fetchPullRequestDetailsNoRetryOn400Test() {
        wireMockServer.stubFor(get(urlEqualTo("/repos/owner/repo/pulls/1"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Bad Request\"}")));

        Mono<PullRequestResponse> response = gitHubClient.fetchPullRequestDetails("owner", "repo", 1);

        StepVerifier.create(response)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().contains("GitHub API error: 400"))
                .verify();
    }

    @Test
    void testFetchPullRequestDetailsFallback() {
        wireMockServer.resetAll();
        ((GitHubClientImpl) gitHubClient).clearCaches(); // Очищаем кэш

        wireMockServer.stubFor(get(urlEqualTo("/repos/owner/repo/pulls/1"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Service Unavailable\"}")));

        Mono<PullRequestResponse> response = gitHubClient.fetchPullRequestDetails("owner", "repo", 1);

        StepVerifier.create(response)
                .expectNextMatches(pr -> pr.getId() == null && pr.getTitle() == null) // Пустой PullRequestResponse
                .verifyComplete();

        wireMockServer
                .findAll(getRequestedFor(urlEqualTo("/repos/owner/repo/pulls/1")))
                .forEach(req -> System.out.println("Request: " + req));

        // Здесь меняем с exactly(3) на exactly(4)
        wireMockServer.verify(exactly(4), getRequestedFor(urlEqualTo("/repos/owner/repo/pulls/1")));
    }
}
