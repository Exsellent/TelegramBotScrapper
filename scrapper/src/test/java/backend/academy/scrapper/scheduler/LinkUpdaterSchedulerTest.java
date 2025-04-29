package backend.academy.scrapper.scheduler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.configuration.ApplicationConfig;
import backend.academy.scrapper.database.scheduler.LinkUpdaterScheduler;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.dto.CombinedPullRequestInfo;
import backend.academy.scrapper.dto.IssuesCommentsResponse;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.dto.LinkUpdateRequest;
import backend.academy.scrapper.dto.PullRequestResponse;
import backend.academy.scrapper.dto.User;
import backend.academy.scrapper.service.ChatLinkService;
import backend.academy.scrapper.service.GitHubService;
import backend.academy.scrapper.service.LinkService;
import backend.academy.scrapper.service.NotificationService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

public class LinkUpdaterSchedulerTest {

    @Mock
    private LinkService linkService;

    @Mock
    private ChatLinkService chatLinkService;

    @Mock
    private ApplicationConfig applicationConfig;

    @Mock
    private ApplicationConfig.Scheduler schedulerConfig;

    @Mock
    private GitHubService gitHubService;

    @Mock
    private NotificationService notificationService;

    private LinkUpdaterScheduler scheduler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        when(applicationConfig.scheduler()).thenReturn(schedulerConfig);
        when(schedulerConfig.enable()).thenReturn(true);
        when(schedulerConfig.interval()).thenReturn(Duration.ofMinutes(5));

        scheduler = new LinkUpdaterScheduler(linkService, chatLinkService, gitHubService, null, notificationService, 5);
    }

    @Test
    public void testUpdateWithGitHubPullRequest() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastUpdate = now.minusHours(1);
        LinkDTO githubLink = LinkDTO.builder()
                .linkId(1L)
                .url("https://github.com/owner/repo/pull/123")
                .description("GitHub PR")
                .createdAt(now)
                .lastCheckTime(now)
                .lastUpdateTime(lastUpdate)
                .tags(null)
                .build();

        doNothing().when(linkService).update(any(LinkDTO.class));

        when(linkService.findLinksToCheck(any(), anyInt(), anyInt()))
                .thenReturn(List.of(githubLink))
                .thenReturn(Collections.emptyList());

        when(chatLinkService.findAllChatsForLink(1L)).thenReturn(List.of(new ChatLinkDTO(1L, 101L)));

        PullRequestResponse pullRequest = new PullRequestResponse();
        pullRequest.setTitle("Test Pull Request");
        pullRequest.setCreatedAt(OffsetDateTime.now().minusDays(1));
        pullRequest.setUser(new User(1L, "testUser", null, "User"));

        User user = new User(1L, "testUser", null, "User");
        IssuesCommentsResponse comment = new IssuesCommentsResponse(
                "https://api.github.com/repos/owner/repo/issues/comments/123",
                123L,
                "This is a test comment",
                user,
                OffsetDateTime.now().minusMinutes(30),
                OffsetDateTime.now().minusMinutes(20));

        CombinedPullRequestInfo prInfo =
                new CombinedPullRequestInfo("Test PR", pullRequest, List.of(comment), new ArrayList<>());

        when(gitHubService.getPullRequestInfo("owner", "repo", 123)).thenReturn(Mono.just(prInfo));

        when(notificationService.sendNotification(any(LinkUpdateRequest.class))).thenReturn(Mono.empty());

        // Act
        scheduler.update();

        // Assert
        verify(gitHubService, times(1)).getPullRequestInfo("owner", "repo", 123);
        verify(notificationService, times(1)).sendNotification(any(LinkUpdateRequest.class));

        ArgumentCaptor<LinkUpdateRequest> captor = ArgumentCaptor.forClass(LinkUpdateRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        assertNotNull(captor.getValue().getDescription());
    }

    @Test
    public void testGitHubUpdateMessageFormatting() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastUpdate = now.minusHours(1);
        LinkDTO githubLink = LinkDTO.builder()
                .linkId(4L)
                .url("https://github.com/owner/repo/pull/456")
                .description("GitHub PR")
                .createdAt(now)
                .lastCheckTime(now)
                .lastUpdateTime(lastUpdate)
                .tags(null)
                .build();

        doNothing().when(linkService).update(any(LinkDTO.class));

        when(linkService.findLinksToCheck(any(), anyInt(), anyInt()))
                .thenReturn(List.of(githubLink))
                .thenReturn(Collections.emptyList());

        when(chatLinkService.findAllChatsForLink(4L)).thenReturn(List.of(new ChatLinkDTO(4L, 404L)));

        PullRequestResponse pullRequest = new PullRequestResponse();
        pullRequest.setTitle("Message Formatting Test PR");
        pullRequest.setCreatedAt(OffsetDateTime.now().minusDays(1));
        pullRequest.setUser(new User(4L, "prUser", null, "User"));

        User user = new User(4L, "commentUser", null, "User");
        String longComment = "This is a very long comment...".repeat(10);
        IssuesCommentsResponse comment = new IssuesCommentsResponse(
                "https://api.github.com/repos/owner/repo/issues/comments/456",
                456L,
                longComment,
                user,
                OffsetDateTime.now().minusMinutes(30),
                OffsetDateTime.now().minusMinutes(10));

        CombinedPullRequestInfo prInfo = new CombinedPullRequestInfo(
                "Message Formatting Test", pullRequest, List.of(comment), new ArrayList<>());

        when(gitHubService.getPullRequestInfo("owner", "repo", 456)).thenReturn(Mono.just(prInfo));

        ArgumentCaptor<LinkUpdateRequest> captor = ArgumentCaptor.forClass(LinkUpdateRequest.class);
        when(notificationService.sendNotification(captor.capture())).thenReturn(Mono.empty());

        // Act
        scheduler.update();

        // Assert
        LinkUpdateRequest capturedRequest = captor.getValue();
        assertNotNull(capturedRequest);
        String description = capturedRequest.getDescription();
        System.out.println("Captured description: " + description);

        assertTrue(description.contains("Comment by commentUser"));

        String[] lines = description.split("\n");
        String commentLine = null;
        for (String line : lines) {
            if (line.startsWith("This is a very long comment")) {
                commentLine = line;
                break;
            }
        }
        assertNotNull(commentLine, "Comment text should be present in description");
        assertTrue(
                commentLine.length() <= 203,
                "Comment preview should be truncated to 200 characters + '...' (203 total)");
        assertTrue(commentLine.endsWith("..."), "Truncated comment should end with ellipsis");
    }

    @Test
    public void testIgnoreFilteredUserUpdates() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastUpdate = now.minusHours(1);
        LinkDTO githubLink = LinkDTO.builder()
                .linkId(1L)
                .url("https://github.com/owner/repo/pull/123")
                .description("GitHub PR")
                .createdAt(now)
                .lastCheckTime(now)
                .lastUpdateTime(lastUpdate)
                .tags(null)
                .build();

        when(linkService.findLinksToCheck(any(), anyInt(), anyInt()))
                .thenReturn(List.of(githubLink))
                .thenReturn(Collections.emptyList());

        when(chatLinkService.findAllChatsForLink(1L)).thenReturn(List.of(new ChatLinkDTO(1L, 101L)));
        when(chatLinkService.getFiltersForLink(1L)).thenReturn(Map.of("user", "testUser"));

        PullRequestResponse pullRequest = new PullRequestResponse();
        pullRequest.setTitle("Test Pull Request");
        pullRequest.setCreatedAt(OffsetDateTime.now().minusDays(1));
        pullRequest.setUser(new User(1L, "testUser", null, "User"));

        User user = new User(1L, "testUser", null, "User");
        IssuesCommentsResponse comment = new IssuesCommentsResponse(
                "https://api.github.com/repos/owner/repo/issues/comments/123",
                123L,
                "This is a test comment",
                user,
                OffsetDateTime.now().minusMinutes(30),
                OffsetDateTime.now().minusMinutes(20));

        CombinedPullRequestInfo prInfo =
                new CombinedPullRequestInfo("Test PR", pullRequest, List.of(comment), new ArrayList<>());

        when(gitHubService.getPullRequestInfo("owner", "repo", 123)).thenReturn(Mono.just(prInfo));

        // Act
        scheduler.update();

        // Assert
        verify(gitHubService, times(1)).getPullRequestInfo("owner", "repo", 123);
        verify(notificationService, never()).sendNotification(any(LinkUpdateRequest.class));
    }
}
