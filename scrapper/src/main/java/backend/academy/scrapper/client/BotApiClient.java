package backend.academy.scrapper.client;

import backend.academy.scrapper.dto.LinkUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class BotApiClient {
    private static final Logger log = LoggerFactory.getLogger(BotApiClient.class);
    private final WebClient webClient;

    @Autowired
    public BotApiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080/api").build();
    }

    public Mono<Void> postUpdate(LinkUpdateRequest update) {
        return webClient
                .post()
                .uri("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(update)
                .retrieve()
                .toBodilessEntity()
                .then();
    }
}
