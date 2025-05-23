package backend.academy.scrapper.client.github;

import backend.academy.scrapper.dto.IssuesCommentsResponse;
import backend.academy.scrapper.dto.PullCommentsResponse;
import backend.academy.scrapper.dto.PullRequestResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Service
public class GitHubClientImpl implements GitHubClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubClientImpl.class);

    private final ConcurrentHashMap<String, Mono<PullRequestResponse>> pullRequestCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Flux<IssuesCommentsResponse>> issueCommentsCache =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Flux<PullCommentsResponse>> pullCommentsCache = new ConcurrentHashMap<>();

    private final WebClient webClient;
    private final Retry retrySpec;

    public GitHubClientImpl(@Qualifier("gitHubWebClient") WebClient webClient, Retry retrySpec) {
        this.webClient = webClient;
        this.retrySpec = retrySpec != null ? retrySpec : Retry.fixedDelay(0, Duration.ZERO);
    }

    public Mono<PullRequestResponse> fetchPullRequestDetails(String owner, String repo, int id, boolean isPullRequest) {
        String type = isPullRequest ? "pull" : "issue";
        String cacheKey = String.format("%s/%s/%d/%s", owner, repo, id, type);

        return pullRequestCache.computeIfAbsent(cacheKey, key -> {
            LOGGER.debug("Fetching {} details for {}", type, key);

            String endpoint = isPullRequest ? "/repos/{owner}/{repo}/pulls/{id}" : "/repos/{owner}/{repo}/issues/{id}";

            return webClient
                    .get()
                    .uri(endpoint, owner, repo, id)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                        LOGGER.error("API error for {}: status code {}", key, response.statusCode());
                        return Mono.error(new RuntimeException("GitHub API error: " + response.statusCode()));
                    })
                    .bodyToMono(PullRequestResponse.class)
                    .publishOn(Schedulers.boundedElastic())
                    .doOnSuccess(pr -> LOGGER.debug("Successfully fetched {} details for {}", type, key))
                    .doOnError(error ->
                            LOGGER.error("Error fetching {} details for {}: {}", type, key, error.getMessage()))
                    .retryWhen(retrySpec)
                    .cache();
        });
    }

    @Override
    public Mono<PullRequestResponse> fetchPullRequestDetails(String owner, String repo, int pullRequestId) {
        return fetchPullRequestDetails(owner, repo, pullRequestId, true);
    }

    public Mono<PullRequestResponse> fetchIssueDetails(String owner, String repo, int issueId) {
        return fetchPullRequestDetails(owner, repo, issueId, false);
    }

    @Override
    public Flux<IssuesCommentsResponse> fetchIssueComments(String owner, String repo, int issueNumber) {
        String cacheKey = String.format("%s/%s/%d/issues/comments", owner, repo, issueNumber);

        return issueCommentsCache.computeIfAbsent(cacheKey, key -> {
            LOGGER.debug("Fetching issue comments for {}", key);

            return webClient
                    .get()
                    .uri("/repos/{owner}/{repo}/issues/{issueNumber}/comments", owner, repo, issueNumber)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                        LOGGER.error("API error for issue comments {}: status code {}", key, response.statusCode());
                        return Mono.error(new RuntimeException("GitHub API error: " + response.statusCode()));
                    })
                    .bodyToFlux(IssuesCommentsResponse.class)
                    .publishOn(Schedulers.boundedElastic())
                    .doOnComplete(() -> LOGGER.debug("Successfully fetched issue comments for {}", key))
                    .doOnError(
                            error -> LOGGER.error("Error fetching issue comments for {}: {}", key, error.getMessage()))
                    .retryWhen(retrySpec)
                    .cache();
        });
    }

    @Override
    public Flux<PullCommentsResponse> fetchPullComments(String owner, String repo, int pullNumber) {
        String cacheKey = String.format("%s/%s/%d/pulls/comments", owner, repo, pullNumber);

        return pullCommentsCache.computeIfAbsent(cacheKey, key -> {
            LOGGER.debug("Fetching pull request comments for {}", key);

            return webClient
                    .get()
                    .uri("/repos/{owner}/{repo}/pulls/{pullNumber}/comments", owner, repo, pullNumber)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                        LOGGER.error("API error for pull comments {}: status code {}", key, response.statusCode());
                        return Mono.error(new RuntimeException("GitHub API error: " + response.statusCode()));
                    })
                    .bodyToFlux(PullCommentsResponse.class)
                    .publishOn(Schedulers.boundedElastic())
                    .doOnComplete(() -> LOGGER.debug("Successfully fetched pull comments for {}", key))
                    .doOnError(
                            error -> LOGGER.error("Error fetching pull comments for {}: {}", key, error.getMessage()))
                    .retryWhen(retrySpec)
                    .cache();
        });
    }

    public void clearCaches() {
        LOGGER.debug("Clearing GitHub client caches");
        pullRequestCache.clear();
        issueCommentsCache.clear();
        pullCommentsCache.clear();
    }
}
