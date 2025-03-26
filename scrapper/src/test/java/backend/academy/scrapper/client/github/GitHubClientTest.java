package backend.academy.scrapper.client.github;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import backend.academy.scrapper.dto.IssuesCommentsResponse;
import backend.academy.scrapper.dto.PullCommentsResponse;
import backend.academy.scrapper.dto.PullRequestResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
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
    private WebClient webClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        webClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .build();

        gitHubClient = new GitHubClientImpl(webClient, null);
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
}
