package backend.academy.scrapper.client.github;

import backend.academy.scrapper.dto.IssuesCommentsResponse;
import backend.academy.scrapper.dto.PullCommentsResponse;
import backend.academy.scrapper.dto.PullRequestResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GitHubClient {
    Mono<PullRequestResponse> fetchPullRequestDetails(String owner, String repo, int pullRequestId);

    Flux<IssuesCommentsResponse> fetchIssueComments(String owner, String repo, int issueNumber);

    Flux<PullCommentsResponse> fetchPullComments(String owner, String repo, int pullNumber);
}
