package backend.academy.bot.controller;

import backend.academy.bot.dto.LinkUpdateRequest;
import backend.academy.bot.insadebot.TelegramBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BotApiController {

    private final TelegramBotService telegramBotService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BotApiController.class);

    @Autowired
    public BotApiController(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @PostMapping("/updates")
    public ResponseEntity<?> postUpdate(@RequestBody LinkUpdateRequest linkUpdate) {
        LOGGER.info("Received update: {}", linkUpdate);

        if (linkUpdate.getUrl() == null || linkUpdate.getUrl().isBlank()) {
            LOGGER.error("Received invalid update: {}", linkUpdate);
            throw new IllegalArgumentException("URL cannot be empty");
        }

        String messageText = String.format("%s\n\n%s", linkUpdate.getUrl(), linkUpdate.getDescription());

        linkUpdate.getTgChatIds().forEach(chatId -> {
            LOGGER.info("Sending update to chat ID {}: {}", chatId, messageText);
            telegramBotService.sendChatMessage(chatId, messageText);
        });

        return ResponseEntity.noContent().build();
    }
}
