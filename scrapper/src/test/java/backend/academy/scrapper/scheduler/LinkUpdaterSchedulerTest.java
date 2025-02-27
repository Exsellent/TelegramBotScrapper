package backend.academy.scrapper.scheduler;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.BotApiClient;
import backend.academy.scrapper.configuration.ApplicationConfig;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.dto.CombinedPullRequestInfo;
import backend.academy.scrapper.dto.IssuesCommentsResponse;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.dto.LinkUpdateRequest;
import backend.academy.scrapper.dto.PullCommentsResponse;
import backend.academy.scrapper.service.ChatLinkService;
import backend.academy.scrapper.service.GitHubService;
import backend.academy.scrapper.service.LinkService;
import backend.academy.scrapper.service.StackOverflowService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

public class LinkUpdaterSchedulerTest {

    @Mock
    private LinkService linkService;

    @Mock
    private ChatLinkService chatLinkService;

    @Mock
    private GitHubService gitHubService;

    @Mock
    private StackOverflowService stackOverflowService;

    @Mock
    private BotApiClient botApiClient;

    @Mock
    private ApplicationConfig applicationConfig;

    @Mock
    private ApplicationConfig.Scheduler schedulerConfig;

    @InjectMocks
    private LinkUpdaterScheduler scheduler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(applicationConfig.scheduler()).thenReturn(schedulerConfig);
        when(schedulerConfig.enable()).thenReturn(true);
        when(schedulerConfig.interval()).thenReturn(Duration.ofDays(1L));

        scheduler = new LinkUpdaterScheduler(
                applicationConfig, linkService, chatLinkService, gitHubService, stackOverflowService, botApiClient);
    }

    @Test
    public void updateTest() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime linkLastUpdateTime = now.minusMinutes(5); // Время последнего обновления ссылки раньше текущего
        LinkDTO mockLink =
                new LinkDTO(1L, "https://github.com/owner/repo/pull/1", "test", now, now, linkLastUpdateTime);

        when(linkService.findLinksToCheck(any(LocalDateTime.class))).thenReturn(Collections.singletonList(mockLink));

        OffsetDateTime commentUpdatedAt = OffsetDateTime.now();
        IssuesCommentsResponse comment = new IssuesCommentsResponse(
                "url", 1L, "Comment body", commentUpdatedAt.minusMinutes(10), commentUpdatedAt);
        List<IssuesCommentsResponse> issueComments = List.of(comment);
        when(gitHubService.getPullRequestInfo(anyString(), anyString(), anyInt()))
                .thenReturn(Mono.just(
                        new CombinedPullRequestInfo("Title", issueComments, new ArrayList<PullCommentsResponse>())));

        // Добавляем мок для chatLinkService, чтобы список tgChatIds не был пустым
        when(chatLinkService.findAllChatsForLink(anyLong())).thenReturn(List.of(new ChatLinkDTO(1L, 1L)));

        when(botApiClient.postUpdate(any(LinkUpdateRequest.class))).thenReturn(Mono.empty());

        // Act
        scheduler.update();

        // Assert
        verify(linkService, times(1)).findLinksToCheck(any(LocalDateTime.class));
        verify(gitHubService, times(1)).getPullRequestInfo(anyString(), anyString(), anyInt());
        verify(botApiClient, times(1)).postUpdate(any(LinkUpdateRequest.class));
    }
}
