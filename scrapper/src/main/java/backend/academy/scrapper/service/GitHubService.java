package backend.academy.scrapper.service;

import backend.academy.scrapper.client.github.GitHubClient;
import backend.academy.scrapper.dto.CombinedPullRequestInfo;
import backend.academy.scrapper.dto.IssuesCommentsResponse;
import backend.academy.scrapper.dto.PullCommentsResponse;
import backend.academy.scrapper.dto.PullRequestResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GitHubService {
    private final GitHubClient gitHubClient;
    private final ChatService chatService;
    private final Timer scrapeTimer;

    @Autowired
    public GitHubService(GitHubClient gitHubClient, ChatService chatService, MeterRegistry meterRegistry) {
        this.gitHubClient = gitHubClient;
        this.chatService = chatService;
        this.scrapeTimer = Timer.builder("scrape_duration_seconds")
                .tag("type", "github")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void registerChat(long chatId) {
        chatService.register(chatId);
    }

    public void unregisterChat(long chatId) {
        chatService.unregister(chatId);
    }

    public Mono<CombinedPullRequestInfo> getPullRequestInfo(String owner, String repo, int pullRequestId) {
        Mono<PullRequestResponse> pullRequestDetailsMono =
                gitHubClient.fetchPullRequestDetails(owner, repo, pullRequestId);

        Mono<List<IssuesCommentsResponse>> issueCommentsMono =
                gitHubClient.fetchIssueComments(owner, repo, pullRequestId).collectList();

        Mono<List<PullCommentsResponse>> pullCommentsMono =
                gitHubClient.fetchPullComments(owner, repo, pullRequestId).collectList();

        return Mono.zip(pullRequestDetailsMono, issueCommentsMono, pullCommentsMono)
                .map(tuple -> new CombinedPullRequestInfo(
                        tuple.getT1().getTitle(), tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }

    public Mono<CombinedPullRequestInfo> getIssueInfo(String owner, String repo, int issueId) {
        Mono<PullRequestResponse> issueDetailsMono = gitHubClient.fetchIssueDetails(owner, repo, issueId);

        Mono<List<IssuesCommentsResponse>> issueCommentsMono =
                gitHubClient.fetchIssueComments(owner, repo, issueId).collectList();

        Mono<List<PullCommentsResponse>> pullCommentsMono = Mono.just(List.of());

        return Mono.zip(issueDetailsMono, issueCommentsMono, pullCommentsMono)
                .map(tuple -> new CombinedPullRequestInfo(
                        tuple.getT1().getTitle(), tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }

    public GitHubClient getGitHubClient() {
        return gitHubClient;
    }

    public void fetchUpdates(String url) {
        scrapeTimer.record(() -> {
            // Парсим URL, например: https://github.com/owner/repo/pull/123 или https://github.com/owner/repo/issues/123
            Pattern pattern = Pattern.compile("https://github\\.com/([^/]+)/([^/]+)/(pull|issues)/(\\d+)");
            Matcher matcher = pattern.matcher(url);
            if (matcher.matches()) {
                String owner = matcher.group(1);
                String repo = matcher.group(2);
                String type = matcher.group(3);
                int id = Integer.parseInt(matcher.group(4));

                if ("pull".equals(type)) {
                    getPullRequestInfo(owner, repo, id).block(); // Блокируем для синхронного вызова
                } else if ("issues".equals(type)) {
                    getIssueInfo(owner, repo, id).block();
                }
            }
        });
    }
}
