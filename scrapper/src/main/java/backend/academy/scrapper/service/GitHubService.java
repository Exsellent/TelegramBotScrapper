package backend.academy.scrapper.service;

import backend.academy.scrapper.client.github.GitHubClient;
import backend.academy.scrapper.dto.CombinedPullRequestInfo;
import backend.academy.scrapper.dto.IssuesCommentsResponse;
import backend.academy.scrapper.dto.PullCommentsResponse;
import backend.academy.scrapper.dto.PullRequestResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GitHubService {
    private final GitHubClient gitHubClient;
    private final ChatService chatService;

    @Autowired
    public GitHubService(GitHubClient gitHubClient, ChatService chatService) {
        this.gitHubClient = gitHubClient;
        this.chatService = chatService;
    }

    public void registerChat(long chatId) {
        chatService.register(chatId);
    }

    public void unregisterChat(long chatId) {
        chatService.unregister(chatId);
    }

    /**
     * Получает информацию о Pull Request.
     *
     * @param owner владелец репозитория
     * @param repo название репозитория
     * @param pullRequestId идентификатор Pull Request
     * @return Mono с комбинированной информацией о Pull Request
     */
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

    /**
     * Получает информацию об Issue.
     *
     * @param owner владелец репозитория
     * @param repo название репозитория
     * @param issueId идентификатор Issue
     * @return Mono с комбинированной информацией об Issue
     */
    public Mono<CombinedPullRequestInfo> getIssueInfo(String owner, String repo, int issueId) {
        Mono<PullRequestResponse> issueDetailsMono = gitHubClient.fetchIssueDetails(owner, repo, issueId);

        Mono<List<IssuesCommentsResponse>> issueCommentsMono =
                gitHubClient.fetchIssueComments(owner, repo, issueId).collectList();

        Mono<List<PullCommentsResponse>> pullCommentsMono =
                Mono.just(List.of()); // Для Issues комментарии к Pull Request не нужны

        return Mono.zip(issueDetailsMono, issueCommentsMono, pullCommentsMono)
                .map(tuple -> new CombinedPullRequestInfo(
                        tuple.getT1().getTitle(), tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }

    /**
     * Возвращает клиент GitHub для очистки кэша.
     *
     * @return GitHubClient
     */
    public GitHubClient getGitHubClient() {
        return gitHubClient;
    }
}
