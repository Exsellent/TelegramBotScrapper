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
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class GitHubServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @InjectMocks
    private GitHubService gitHubService;

    @BeforeEach
    public void setup() {
        OffsetDateTime now = OffsetDateTime.now();

        // Создание мок-объектов
        PullRequestResponse mockPullRequestResponse = new PullRequestResponse();
        mockPullRequestResponse.setTitle("Test PR");
        mockPullRequestResponse.setCreatedAt(now);
        mockPullRequestResponse.setUpdatedAt(now);
        // Если есть поле id, можно раскомментировать:
        // mockPullRequestResponse.setId(1L);

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

        // Настройка моков
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
