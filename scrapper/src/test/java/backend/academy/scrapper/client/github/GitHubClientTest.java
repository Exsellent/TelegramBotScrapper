package backend.academy.scrapper.client.github;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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
import org.springframework.core.codec.DecodingException;
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
        wireMockServer = new WireMockServer(wireMockConfig().port(8089));
        wireMockServer.start();
        configureFor("localhost", 8089);

        WebClient webClient =
                WebClient.builder().baseUrl("http://localhost:8089").build();
        gitHubClient = new GitHubClientImpl(webClient, null);

        setUpPullRequestInfo();
        setUpIssueComments();
        setUpPullComments();
    }

    void setUpPullRequestInfo() {
        wireMockServer.stubFor(
                get(urlEqualTo("/repos/owner/repo/pulls/1"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(
                                                "{\"id\": 1, \"title\": \"Test PR\", \"review_comments_url\": \"\", \"comments_url\": \"\"}")));
    }

    void setUpIssueComments() {
        wireMockServer.stubFor(
                get(urlEqualTo("/repos/owner/repo/issues/1/comments"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(
                                                "[{\"id\": 2, \"url\": \"http://example.com\", \"created_at\": \"2020-01-01T00:00:00Z\", \"body\": \"Test comment\"}]")));
    }

    void setUpPullComments() {
        wireMockServer.stubFor(
                get(urlEqualTo("/repos/owner/repo/pulls/1/comments"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(
                                                "[{\"id\": 3, \"url\": \"http://example.com\", \"created_at\": \"2020-01-02T00:00:00Z\", \"body\": \"Test pull comment\"}]")));
    }

    @Test
    void fetchPullRequestDetailsTest() {
        Mono<PullRequestResponse> response = gitHubClient.fetchPullRequestDetails("owner", "repo", 1);
        StepVerifier.create(response)
                .expectNextMatches(pr -> pr.getId() == 1 && pr.getTitle().equals("Test PR"))
                .verifyComplete();
    }

    @Test
    void fetchIssueCommentsTest() {
        Flux<IssuesCommentsResponse> response = gitHubClient.fetchIssueComments("owner", "repo", 1);
        StepVerifier.create(response)
                .expectNextMatches(
                        comment -> comment.getId() == 2 && comment.getBody().equals("Test comment"))
                .verifyComplete();
    }

    @Test
    void fetchPullCommentsTest() {
        Flux<PullCommentsResponse> response = gitHubClient.fetchPullComments("owner", "repo", 1);
        StepVerifier.create(response)
                .expectNextMatches(
                        comment -> comment.getId() == 3 && comment.getBody().equals("Test pull comment"))
                .verifyComplete();
    }

    @Test
    void fetchPullRequestDetails_NotFound() {
        wireMockServer.stubFor(get(urlEqualTo("/repos/owner/repo/pulls/1"))
                .willReturn(aResponse().withStatus(404)));

        Mono<PullRequestResponse> response = gitHubClient.fetchPullRequestDetails("owner", "repo", 1);

        StepVerifier.create(response)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("API Error"))
                .verify();
    }

    @Test
    void fetchIssueComments_InternalServerError() {
        wireMockServer.stubFor(get(urlEqualTo("/repos/owner/repo/issues/1/comments"))
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        Flux<IssuesCommentsResponse> response = gitHubClient.fetchIssueComments("owner", "repo", 1);

        StepVerifier.create(response)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("API Error"))
                .verify();
    }

    @Test
    void fetchPullComments_InvalidJsonResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/repos/owner/repo/pulls/1/comments"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{invalid_json}"))); // Некорректный JSON

        Flux<PullCommentsResponse> response = gitHubClient.fetchPullComments("owner", "repo", 1);

        StepVerifier.create(response)
                .expectErrorMatches(throwable -> throwable instanceof DecodingException)
                .verify();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }
}
