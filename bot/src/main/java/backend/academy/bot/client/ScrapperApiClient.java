package backend.academy.bot.client;

import backend.academy.bot.dto.AddLinkRequest;
import backend.academy.bot.dto.LinkResponse;
import backend.academy.bot.dto.LinkUpdateRequest;
import backend.academy.bot.dto.ListLinksResponse;
import backend.academy.bot.dto.RemoveLinkRequest;
import backend.academy.bot.exception.ApiException;
import backend.academy.bot.exception.FilterValidationException;
import backend.academy.bot.exception.InvalidLinkException;
import backend.academy.bot.utils.LinkParser;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ScrapperApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScrapperApiClient.class);
    private final RestClient restClient;
    private final String baseUrl;

    private static final int REQUEST_TIMEOUT = 30000; // 30 seconds timeout
    private static final int MAX_RETRIES = 3;

    @Value("${conversation.timeout.minutes:15}")
    private int conversationTimeoutMinutes;


    public ScrapperApiClient(@Value("${scrapper.api.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultStatusHandler((HttpStatusCode status) -> status.is4xxClientError(), (request, response) -> {
                    throw new ApiException("Client error: " + response.getStatusCode());
                })
                .defaultStatusHandler((HttpStatusCode status) -> status.is5xxServerError(), (request, response) -> {
                    throw new ApiException("Server error: " + response.getStatusCode());
                })
                .build();
    }

    @PostConstruct
    public void init() {
        try {
            restClient.get().uri(baseUrl + "/actuator/health").retrieve().toBodilessEntity();
            LOGGER.info("The Scrapper API is available at: {}", baseUrl);
        } catch (Exception e) {
            LOGGER.warn("The Scrapper API is unavailable at: {}. Error: {}", baseUrl, e.getMessage());
        }
    }

    public void addLink(Long chatId, AddLinkRequest requestPayload) {
        validateLinkRequest(requestPayload);

        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(MAX_RETRIES)
                .fixedBackoff(1000) // 1 second between retries
                .retryOn(ApiException.class)
                .build();

        try {
            retryTemplate.execute(context -> {
                LOGGER.debug(
                        "Attempting to add link: {} for chat: {}, attempt: {}",
                        requestPayload.getLink(),
                        chatId,
                        context.getRetryCount() + 1);

                return restClient
                        .post()
                        .uri(baseUrl + "/chats/{chatId}/links", chatId)
                        .body(requestPayload)
                        .retrieve()
                        .toBodilessEntity();
            });

            LOGGER.info("Successfully added link: {} for chat: {}", requestPayload.getLink(), chatId);
        } catch (Exception e) {
            LOGGER.error("Failed to add link after {} retries: {}", MAX_RETRIES, e.getMessage());
            throw new ApiException("Failed to add link: " + e.getMessage(), e);
        }
    }

    private void validateLinkRequest(AddLinkRequest request) {
        if (!LinkParser.isValidURL(request.getLink())) {
            throw new InvalidLinkException("Invalid URL format: " + request.getLink());
        }

        if (request.getFilters() != null) {
            validateFilters(request.getFilters());
        }

        if (request.getTags() != null) {
            validateTags(request.getTags());
        }
    }

    private void validateFilters(Map<String, String> filters) {
        Set<String> allowedFilterKeys = Set.of("user", "type", "label", "state");

        for (Map.Entry<String, String> filter : filters.entrySet()) {
            if (!allowedFilterKeys.contains(filter.getKey())) {
                throw new FilterValidationException("Invalid filter key: " + filter.getKey());
            }

            if (filter.getValue() == null || filter.getValue().trim().isEmpty()) {
                throw new FilterValidationException("Filter value cannot be empty for key: " + filter.getKey());
            }
        }
    }

    private void validateTags(List<String> tags) {
        if (tags.stream().anyMatch(tag -> tag == null || tag.trim().isEmpty())) {
            throw new FilterValidationException("Tags cannot be empty or null");
        }
    }

    public boolean isConversationTimedOut(Long chatId, LocalDateTime startTime) {
        return startTime.plusMinutes(conversationTimeoutMinutes).isBefore(LocalDateTime.now());
    }

    public void removeLink(Long chatId, RemoveLinkRequest requestPayload) {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(MAX_RETRIES)
                .fixedBackoff(1000)
                .retryOn(ApiException.class)
                .build();

        try {
            retryTemplate.execute(context -> {
                return restClient
                        .method(HttpMethod.DELETE)
                        .uri(baseUrl + "/chats/{chatId}/links", chatId)
                        .body(requestPayload)
                        .retrieve()
                        .toBodilessEntity();
            });

            LOGGER.info("Successfully removed link: {} for chat: {}", requestPayload.getLink(), chatId);
        } catch (Exception e) {
            LOGGER.error("Failed to remove link after {} retries: {}", MAX_RETRIES, e.getMessage());
            throw new ApiException("Failed to remove link: " + e.getMessage(), e);
        }
    }

    public void cancelOperation(Long chatId) {
        try {
            restClient
                    .post()
                    .uri(baseUrl + "/chats/{chatId}/cancel", chatId)
                    .retrieve()
                    .toBodilessEntity();

            LOGGER.info("Successfully cancelled operation for chat: {}", chatId);
        } catch (Exception e) {
            LOGGER.error("Failed to cancel operation: {}", e.getMessage());
            throw new ApiException("Failed to cancel operation: " + e.getMessage(), e);
        }
    }

    public List<LinkUpdateRequest> getUpdates() {
        try {
            return restClient
                    .get()
                    .uri(baseUrl + "/updates")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<LinkUpdateRequest>>() {});
        } catch (Exception e) {
            LOGGER.error("Error when receiving updates: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public ListLinksResponse getAllLinks(Long chatId) {
        try {
            return restClient
                    .get()
                    .uri(baseUrl + "/chats/{chatId}/links", chatId)
                    .retrieve()
                    .body(ListLinksResponse.class);
        } catch (Exception e) {
            LOGGER.error("Error when getting the list of links: {}", e.getMessage());
            return new ListLinksResponse(Collections.emptyList());
        }
    }

    public void registerChat(Long chatId) {
        try {
            restClient
                    .post()
                    .uri(baseUrl + "/chats/{chatId}", chatId)
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("Chat {} registered.", chatId);
        } catch (Exception e) {
            LOGGER.error("Error registering the chat {}: {}", chatId, e.getMessage());
        }
    }


}
