package backend.academy.scrapper.client.github;

import backend.academy.scrapper.dto.IssuesCommentsResponse;
import backend.academy.scrapper.dto.PullCommentsResponse;
import backend.academy.scrapper.dto.PullRequestResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Интерфейс клиента для работы с GitHub API. */
public interface GitHubClient {

    /**
     * Получает информацию о Pull Request по его идентификатору.
     *
     * @param owner владелец репозитория
     * @param repo название репозитория
     * @param pullRequestId идентификатор Pull Request
     * @return Mono с информацией о Pull Request
     */
    Mono<PullRequestResponse> fetchPullRequestDetails(String owner, String repo, int pullRequestId);

    /**
     * Получает информацию об Issue по его идентификатору.
     *
     * @param owner владелец репозитория
     * @param repo название репозитория
     * @param issueId идентификатор Issue
     * @return Mono с информацией об Issue
     */
    Mono<PullRequestResponse> fetchIssueDetails(String owner, String repo, int issueId);

    /**
     * Получает комментарии к Issue.
     *
     * @param owner владелец репозитория
     * @param repo название репозитория
     * @param issueNumber идентификатор Issue
     * @return Flux с комментариями к Issue
     */
    Flux<IssuesCommentsResponse> fetchIssueComments(String owner, String repo, int issueNumber);

    /**
     * Получает комментарии к Pull Request.
     *
     * @param owner владелец репозитория
     * @param repo название репозитория
     * @param pullNumber идентификатор Pull Request
     * @return Flux с комментариями к Pull Request
     */
    Flux<PullCommentsResponse> fetchPullComments(String owner, String repo, int pullNumber);

    /** Очищает кэши клиента. */
    void clearCaches();
}
