package backend.academy.scrapper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.github.GitHubClient;
import backend.academy.scrapper.dto.CombinedPullRequestInfo;
import backend.academy.scrapper.dto.IssuesCommentsResponse;
import backend.academy.scrapper.dto.PullCommentsResponse;
import backend.academy.scrapper.dto.PullRequestResponse;
import backend.academy.scrapper.dto.User;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class GitHubServiceTest {

    private GitHubService gitHubService;
    private GitHubClient gitHubClient;
    private ChatService chatService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    public void setup() {
        gitHubClient = Mockito.mock(GitHubClient.class);
        chatService = Mockito.mock(ChatService.class);
        meterRegistry = new SimpleMeterRegistry();

        gitHubService = new GitHubService(gitHubClient, chatService, meterRegistry);

        OffsetDateTime now = OffsetDateTime.now();

        PullRequestResponse mockPullRequestResponse = new PullRequestResponse();
        mockPullRequestResponse.setTitle("Test PR");
        mockPullRequestResponse.setCreatedAt(now);
        mockPullRequestResponse.setUpdatedAt(now);

        IssuesCommentsResponse mockIssueComment = new IssuesCommentsResponse(
                "https://api.github.com/issue/comment",
                1L,
                "Issue Comment",
                new User(null, "testUser", null, null),
                now,
                now);

        PullCommentsResponse mockPullComment = new PullCommentsResponse(
                "https://api.github.com/pull/comment",
                2L,
                "Pull Comment",
                new User(null, "testUser", null, null),
                now,
                now);

        when(gitHubClient.fetchPullRequestDetails(anyString(), anyString(), anyInt()))
                .thenReturn(Mono.just(mockPullRequestResponse));

        when(gitHubClient.fetchIssueComments(anyString(), anyString(), anyInt()))
                .thenReturn(Flux.just(mockIssueComment));

        when(gitHubClient.fetchPullComments(anyString(), anyString(), anyInt())).thenReturn(Flux.just(mockPullComment));
    }

    @Test
    public void testGetPullRequestInfo() {
        Mono<CombinedPullRequestInfo> result = gitHubService.getPullRequestInfo("owner", "repo", 1);

        StepVerifier.create(result)
                .assertNext(combinedInfo -> {
                    assertEquals("Test PR", combinedInfo.getTitle());
                    assertEquals(1, combinedInfo.getIssueComments().size());
                    assertEquals(1, combinedInfo.getPullComments().size());
                    assertEquals(
                            "Issue Comment",
                            combinedInfo.getIssueComments().get(0).getBody());
                    assertEquals(
                            "Pull Comment",
                            combinedInfo.getPullComments().get(0).getBody());
                })
                .verifyComplete();
    }
}
