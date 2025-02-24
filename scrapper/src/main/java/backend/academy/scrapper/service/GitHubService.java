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

    public Mono<CombinedPullRequestInfo> getPullRequestInfo(String owner, String repo, int pullRequestId) {
        Mono<PullRequestResponse> pullRequestDetailsMono =
                gitHubClient.fetchPullRequestDetails(owner, repo, pullRequestId);
        Mono<List<IssuesCommentsResponse>> issueCommentsMono =
                gitHubClient.fetchIssueComments(owner, repo, pullRequestId).collectList();
        Mono<List<PullCommentsResponse>> pullCommentsMono =
                gitHubClient.fetchPullComments(owner, repo, pullRequestId).collectList();

        return Mono.zip(pullRequestDetailsMono, issueCommentsMono, pullCommentsMono)
                .map(tuple -> new CombinedPullRequestInfo(tuple.getT1().getTitle(), tuple.getT2(), tuple.getT3()));
    }
}
