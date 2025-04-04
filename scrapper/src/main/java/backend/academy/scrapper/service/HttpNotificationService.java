package backend.academy.scrapper.service;

import backend.academy.scrapper.client.BotApiClient;
import backend.academy.scrapper.dto.LinkUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.message-transport", havingValue = "HTTP")
public class HttpNotificationService implements NotificationService {
    private final BotApiClient botApiClient;

    @Autowired
    public HttpNotificationService(BotApiClient botApiClient) {
        this.botApiClient = botApiClient;
    }

    @Override
    public void sendNotification(LinkUpdateRequest update) {
        botApiClient.postUpdate(update); // Исправил sendUpdate на postUpdate
    }
}
