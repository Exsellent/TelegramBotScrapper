package backend.academy.scrapper.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.dto.LinkUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class BotApiClientTest {

    @Mock
    private WebClient.Builder webClientBuilderMock;

    @Mock
    private WebClient webClientMock;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;

    @Mock
    private WebClient.RequestBodySpec requestBodySpecMock;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpecMock;

    @Mock
    private WebClient.ResponseSpec responseSpecMock;

    private BotApiClient botApiClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Настраиваем мок WebClient.Builder
        when(webClientBuilderMock.baseUrl(anyString())).thenReturn(webClientBuilderMock);
        when(webClientBuilderMock.build()).thenReturn(webClientMock);

        // Настраиваем цепочку моков для WebClient
        when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri("/updates")).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.bodyValue(any(LinkUpdateRequest.class))).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.toBodilessEntity()).thenReturn(Mono.empty());

        // Создаем тестируемый объект с мокнутым WebClient.Builder
        botApiClient = new BotApiClient(webClientBuilderMock);
    }

    @Test
    void testPostUpdate() {
        // Создаем тестовый запрос
        LinkUpdateRequest linkUpdateRequest = new LinkUpdateRequest();

        // Вызываем тестируемый метод
        Mono<Void> resultMono = botApiClient.postUpdate(linkUpdateRequest);

        // Проверяем, что Mono завершается успешно
        resultMono.block(); // Блокирующий вызов для проверки успешного завершения
    }
}
