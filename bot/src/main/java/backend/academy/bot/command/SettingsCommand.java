package backend.academy.bot.command;

import backend.academy.bot.service.RedisCacheService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SettingsCommand implements Command {
    private static final int EXPECTED_PARTS_COUNT = 2;
    private static final int MODE_INDEX = 1;
    private final RedisCacheService redisCacheService;

    @Autowired
    public SettingsCommand(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    @Override
    public String command() {
        return "/settings";
    }

    @Override
    public String description() {
        return "Set notification mode: instant or digest";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        String[] parts = update.message().text().split(" ", EXPECTED_PARTS_COUNT);

        if (parts.length < EXPECTED_PARTS_COUNT
                || (!parts[MODE_INDEX].equals("instant") && !parts[MODE_INDEX].equals("digest"))) {
            return new SendMessage(chatId, "Usage: /settings [instant|digest]");
        }

        String mode = parts[MODE_INDEX];
        redisCacheService.setNotificationMode(chatId, mode);
        return new SendMessage(chatId, "Notification mode set to: " + mode);
    }
}
