package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.LinkUpdateRequest;

public interface NotificationService {
    void sendNotification(LinkUpdateRequest update);
}
