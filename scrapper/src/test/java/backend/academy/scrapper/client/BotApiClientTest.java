package backend.academy.scrapper.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.dto.LinkUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class BotApiClientTest {

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

        // Настраиваем цепочку моков
        when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri("/updates")).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.bodyValue(any(LinkUpdateRequest.class))).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);

        // Создаем мок для ClientResponse
        ClientResponse clientResponseMock = mock(ClientResponse.class);
        when(responseSpecMock.toBodilessEntity()).thenReturn(Mono.empty());

        // Создаем тестируемый объект с моком
        botApiClient = new BotApiClient(webClientMock);
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
