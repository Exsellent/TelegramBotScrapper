package backend.academy.scrapper.client.client.stackoverflow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.ResilienceUtils;
import backend.academy.scrapper.client.stackoverflow.StackOverflowClientImpl;
import backend.academy.scrapper.dto.QuestionResponse;
import backend.academy.scrapper.dto.QuestionsApiResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class StackOverflowClientImplTest {

    private WebClient webClient;
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    private WebClient.ResponseSpec responseSpec;

    private ResilienceUtils resilienceUtils;
    private StackOverflowClientImpl client;

    @BeforeEach
    void setUp() {
        webClient = mock(WebClient.class);
        requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        resilienceUtils = new ResilienceUtils(3, 0, "503,429");
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        client = new StackOverflowClientImpl(webClient, resilienceUtils, registry);
    }

    @Test
    void shouldHandleNonRetryableErrorWithoutRetry() {
        WebClient isolatedWebClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec isolatedUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec isolatedHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec isolatedResponseSpec = mock(WebClient.ResponseSpec.class);

        when(isolatedWebClient.get()).thenReturn(isolatedUriSpec);
        when(isolatedUriSpec.uri(any(Function.class))).thenReturn(isolatedHeaderSpec);
        when(isolatedHeaderSpec.retrieve()).thenReturn(isolatedResponseSpec);

        List<String> ids = List.of("404");

        WebClientResponseException nonRetryableException =
                new WebClientResponseException(404, "Not Found", null, new byte[0], StandardCharsets.UTF_8);

        when(isolatedResponseSpec.bodyToMono(QuestionsApiResponse.class)).thenReturn(Mono.error(nonRetryableException));

        StackOverflowClientImpl isolatedClient =
                new StackOverflowClientImpl(isolatedWebClient, resilienceUtils, CircuitBreakerRegistry.ofDefaults());

        StepVerifier.create(isolatedClient.fetchQuestionsInfo(ids))
                .expectError(WebClientResponseException.class)
                .verify();

        verify(isolatedResponseSpec, times(1)).bodyToMono(QuestionsApiResponse.class);
    }

    @Test
    void shouldReturnSuccessfulResponse() {
        WebClient successWebClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec successUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec successHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec successResponseSpec = mock(WebClient.ResponseSpec.class);

        when(successWebClient.get()).thenReturn(successUriSpec);
        when(successUriSpec.uri(any(Function.class))).thenReturn(successHeaderSpec);
        when(successHeaderSpec.retrieve()).thenReturn(successResponseSpec);

        List<String> ids = List.of("789");

        QuestionResponse question = new QuestionResponse();
        QuestionsApiResponse apiResponse = new QuestionsApiResponse();
        apiResponse.setItems(List.of(question));

        when(successResponseSpec.bodyToMono(QuestionsApiResponse.class)).thenReturn(Mono.just(apiResponse));

        StackOverflowClientImpl successClient =
                new StackOverflowClientImpl(successWebClient, resilienceUtils, CircuitBreakerRegistry.ofDefaults());

        StepVerifier.create(successClient.fetchQuestionsInfo(ids))
                .expectNext(List.of(question))
                .verifyComplete();

        verify(successResponseSpec, times(1)).bodyToMono(QuestionsApiResponse.class);
    }

    @Test
    void shouldUseCacheForRepeatedRequests() {
        WebClient cacheWebClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec cacheUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec cacheHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec cacheResponseSpec = mock(WebClient.ResponseSpec.class);

        when(cacheWebClient.get()).thenReturn(cacheUriSpec);
        when(cacheUriSpec.uri(any(Function.class))).thenReturn(cacheHeaderSpec);
        when(cacheHeaderSpec.retrieve()).thenReturn(cacheResponseSpec);

        List<String> ids = List.of("cached");

        QuestionResponse question = new QuestionResponse();
        QuestionsApiResponse apiResponse = new QuestionsApiResponse();
        apiResponse.setItems(List.of(question));

        when(cacheResponseSpec.bodyToMono(QuestionsApiResponse.class)).thenReturn(Mono.just(apiResponse));

        StackOverflowClientImpl cacheClient =
                new StackOverflowClientImpl(cacheWebClient, resilienceUtils, CircuitBreakerRegistry.ofDefaults());

        StepVerifier.create(cacheClient.fetchQuestionsInfo(ids))
                .expectNext(List.of(question))
                .verifyComplete();

        StepVerifier.create(cacheClient.fetchQuestionsInfo(ids))
                .expectNext(List.of(question))
                .verifyComplete();

        verify(cacheResponseSpec, times(1)).bodyToMono(QuestionsApiResponse.class);
    }
}
