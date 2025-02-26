package backend.academy.bot.command;

import backend.academy.bot.client.ScrapperApiClient;
import backend.academy.bot.dto.LinkUpdateRequest;
import backend.academy.bot.insidebot.TelegramBotService;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UpdateScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateScheduler.class);
    private final ScrapperApiClient scrapperApiClient;
    private final TelegramBotService bot;

    @Autowired
    public UpdateScheduler(ScrapperApiClient scrapperApiClient, TelegramBotService bot) {
        this.scrapperApiClient = scrapperApiClient;
        this.bot = bot;
    }

    @Scheduled(fixedRate = 60000) // Запуск каждую минуту
    public void checkUpdates() {
        try {
            List<LinkUpdateRequest> updates = scrapperApiClient.getUpdates();

            if (updates == null || updates.isEmpty()) {
                LOGGER.info("There are no new updates.");
                return;
            }

            for (LinkUpdateRequest update : updates) {
                for (Long chatId : update.getTgChatIds()) {
                    SendMessage message = new SendMessage(chatId, formatUpdateMessage(update));
                    bot.execute(message);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error checking for updates: {}", e.getMessage(), e);

            // Задержка 5 секунд перед следующим запросом, чтобы избежать перегрузки системы
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String formatUpdateMessage(LinkUpdateRequest update) {
        return String.format("Detected changes in %s:\n%s", update.getUrl(), update.getDescription());
    }
}
