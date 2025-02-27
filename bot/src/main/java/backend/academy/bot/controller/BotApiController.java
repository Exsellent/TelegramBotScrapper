package backend.academy.bot.controller;

import backend.academy.bot.dto.LinkUpdateRequest;
import backend.academy.bot.insidebot.TelegramBotService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class BotApiController {

    private final TelegramBotService telegramBotService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BotApiController.class);

    @Autowired
    public BotApiController(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @PostMapping("/updates")
    public ResponseEntity<?> postUpdate(@Valid @RequestBody LinkUpdateRequest linkUpdate) { // @Valid добавлен
        LOGGER.info("Received update: {}", linkUpdate);

        String messageText = String.format("%s\n\n%s", linkUpdate.getUrl(), linkUpdate.getDescription());

        linkUpdate.getTgChatIds().forEach(chatId -> {
            LOGGER.info("Sending update to chat ID {}: {}", chatId, messageText);
            telegramBotService.sendChatMessage(chatId, messageText);
        });

        return ResponseEntity.noContent().build();
    }
}
