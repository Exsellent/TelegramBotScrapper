package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.LinkUpdateRequest;
import reactor.core.publisher.Mono;

public interface NotificationService {
    Mono<Void> sendNotification(LinkUpdateRequest update);
}
