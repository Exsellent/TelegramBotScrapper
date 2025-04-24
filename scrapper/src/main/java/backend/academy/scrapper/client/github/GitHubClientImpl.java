package backend.academy.scrapper.client.github;

import backend.academy.scrapper.dto.IssuesCommentsResponse;
import backend.academy.scrapper.dto.PullCommentsResponse;
import backend.academy.scrapper.dto.PullRequestResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

public class GitHubClientImpl implements GitHubClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubClientImpl.class);

    private final WebClient webClient;
    private final Retry retrySpec;
    private final CircuitBreaker circuitBreaker;

    private final ConcurrentMap<String, Mono<PullRequestResponse>> pullRequestCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Flux<IssuesCommentsResponse>> issueCommentsCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Flux<PullCommentsResponse>> pullCommentsCache = new ConcurrentHashMap<>();

    public GitHubClientImpl(
            WebClient webClient, int maxAttempts, long backoffSeconds, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = webClient;
        this.retrySpec = Retry.fixedDelay(maxAttempts, Duration.ofSeconds(backoffSeconds))
                .filter(throwable -> {
                    if (throwable instanceof WebClientResponseException ex) {
                        return List.of(500, 502, 503, 504)
                                .contains(ex.getStatusCode().value());
                    }
                    return false;
                })
                .doBeforeRetry(
                        signal -> LOGGER.debug("Retrying GitHub request, attempt: {}", signal.totalRetries() + 1));
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("gitHubClient");
    }

    @Override
    public Mono<PullRequestResponse> fetchPullRequestDetails(String owner, String repo, int id) {
        return fetchPullRequestDetails(owner, repo, id, true);
    }

    public Mono<PullRequestResponse> fetchIssueDetails(String owner, String repo, int issueId) {
        return fetchPullRequestDetails(owner, repo, issueId, false);
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
                    .bodyToMono(PullRequestResponse.class)
                    .retryWhen(retrySpec)
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    .publishOn(Schedulers.boundedElastic())
                    .doOnSuccess(pr -> LOGGER.debug("Successfully fetched {} details for {}", type, key))
                    .doOnError(error ->
                            LOGGER.error("Error fetching {} details for {}: {}", type, key, error.getMessage()))
                    .onErrorMap(throwable -> {
                        if (throwable instanceof WebClientResponseException ex) {
                            LOGGER.error(
                                    "API error for {}: status code {}, response body: {}",
                                    key,
                                    ex.getStatusCode(),
                                    ex.getResponseBodyAsString());
                            return new RuntimeException("GitHub API error: " + ex.getStatusCode(), ex);
                        }
                        return throwable;
                    })
                    .cache();
        });
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
                    .bodyToFlux(IssuesCommentsResponse.class)
                    .retryWhen(retrySpec)
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    .publishOn(Schedulers.boundedElastic())
                    .doOnComplete(() -> LOGGER.debug("Successfully fetched issue comments for {}", key))
                    .doOnError(
                            error -> LOGGER.error("Error fetching issue comments for {}: {}", key, error.getMessage()))
                    .onErrorMap(throwable -> {
                        if (throwable instanceof WebClientResponseException ex) {
                            LOGGER.error(
                                    "API error for issue comments {}: status code {}, response body: {}",
                                    key,
                                    ex.getStatusCode(),
                                    ex.getResponseBodyAsString());
                            return new RuntimeException("GitHub API error: " + ex.getStatusCode(), ex);
                        }
                        return throwable;
                    })
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
                    .bodyToFlux(PullCommentsResponse.class)
                    .retryWhen(retrySpec)
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    .publishOn(Schedulers.boundedElastic())
                    .doOnComplete(() -> LOGGER.debug("Successfully fetched pull comments for {}", key))
                    .doOnError(
                            error -> LOGGER.error("Error fetching pull comments for {}: {}", key, error.getMessage()))
                    .onErrorMap(throwable -> {
                        if (throwable instanceof WebClientResponseException ex) {
                            LOGGER.error(
                                    "API error for pull comments {}: status code {}, response body: {}",
                                    key,
                                    ex.getStatusCode(),
                                    ex.getResponseBodyAsString());
                            return new RuntimeException("GitHub API error: " + ex.getStatusCode(), ex);
                        }
                        return throwable;
                    })
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
