package backend.academy.scrapper.client.github;

import backend.academy.scrapper.dto.IssuesCommentsResponse;
import backend.academy.scrapper.dto.PullCommentsResponse;
import backend.academy.scrapper.dto.PullRequestResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class GitHubClientImpl implements GitHubClient {

    private final WebClient webClient;
    private final Retry retrySpec;

    public GitHubClientImpl(@Qualifier("gitHubWebClient") WebClient webClient, Retry retrySpec) {
        this.webClient = webClient;
        this.retrySpec = retrySpec;
    }

    @Override
    public Mono<PullRequestResponse> fetchPullRequestDetails(String owner, String repo, int pullRequestId) {
        Mono<PullRequestResponse> mono = webClient
                .get()
                .uri("/repos/{owner}/{repo}/pulls/{pullRequestId}", owner, repo, pullRequestId)
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError()
                            || response.statusCode().is5xxServerError()) {
                        return Mono.error(new RuntimeException("API Error"));
                    }
                    return response.bodyToMono(PullRequestResponse.class);
                });
        return retrySpec != null ? mono.retryWhen(retrySpec) : mono;
    }

    @Override
    public Flux<IssuesCommentsResponse> fetchIssueComments(String owner, String repo, int issueNumber) {
        Flux<IssuesCommentsResponse> flux = webClient
                .get()
                .uri("/repos/{owner}/{repo}/issues/{issueNumber}/comments", owner, repo, issueNumber)
                .exchangeToFlux(response -> {
                    if (response.statusCode().is4xxClientError()
                            || response.statusCode().is5xxServerError()) {
                        return Flux.error(new RuntimeException("API Error"));
                    }
                    return response.bodyToFlux(IssuesCommentsResponse.class);
                });
        return retrySpec != null ? flux.retryWhen(retrySpec) : flux;
    }

    @Override
    public Flux<PullCommentsResponse> fetchPullComments(String owner, String repo, int pullNumber) {
        Flux<PullCommentsResponse> flux = webClient
                .get()
                .uri("/repos/{owner}/{repo}/pulls/{pullNumber}/comments", owner, repo, pullNumber)
                .exchangeToFlux(response -> {
                    if (response.statusCode().is4xxClientError()
                            || response.statusCode().is5xxServerError()) {
                        return Flux.error(new RuntimeException("API Error"));
                    }
                    return response.bodyToFlux(PullCommentsResponse.class);
                });
        return retrySpec != null ? flux.retryWhen(retrySpec) : flux;
    }
}
