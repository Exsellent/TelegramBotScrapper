package backend.academy.scrapper.service;

import backend.academy.scrapper.client.github.GitHubClient;
import backend.academy.scrapper.dto.CombinedPullRequestInfo;
import backend.academy.scrapper.dto.IssuesCommentsResponse;
import backend.academy.scrapper.dto.PullCommentsResponse;
import backend.academy.scrapper.dto.PullRequestResponse;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class GitHubServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @InjectMocks
    private GitHubService gitHubService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        OffsetDateTime now = OffsetDateTime.now();

        // Создание мок-объектов с правильными аргументами
        PullRequestResponse mockPullRequestResponse = new PullRequestResponse("Test PR", now, now, 1L);
        IssuesCommentsResponse mockIssueComment = new IssuesCommentsResponse(
            "https://api.github.com/issue/comment", 1L, "Issue Comment", now, now);
        PullCommentsResponse mockPullComment = new PullCommentsResponse(
            "https://api.github.com/pull/comment", 2L, "Mock Diff Hunk", now, now, "Pull Comment");

        // Настройка моков
        when(gitHubClient.fetchPullRequestDetails(anyString(), anyString(), anyInt()))
            .thenReturn(Mono.just(mockPullRequestResponse));
        when(gitHubClient.fetchIssueComments(anyString(), anyString(), anyInt()))
            .thenReturn(Flux.just(mockIssueComment));
        when(gitHubClient.fetchPullComments(anyString(), anyString(), anyInt()))
            .thenReturn(Flux.just(mockPullComment));
    }

    @Test
    public void testGetPullRequestInfo() {
        Mono<CombinedPullRequestInfo> result = gitHubService.getPullRequestInfo("owner", "repo", 1);

        StepVerifier.create(result)
            .assertNext(combinedInfo -> {
                assert combinedInfo.getTitle().equals("Test PR");
                assert combinedInfo.getIssueComments().size() == 1;
                assert combinedInfo.getPullComments().size() == 1;
                assert combinedInfo.getIssueComments().get(0).getBody().equals("Issue Comment");
                assert combinedInfo.getPullComments().get(0).getBody().equals("Pull Comment");
            })
            .verifyComplete();
    }
}
