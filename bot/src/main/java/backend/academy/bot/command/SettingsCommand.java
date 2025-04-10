package backend.academy.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SettingsCommand implements Command {
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public SettingsCommand(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
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
        String[] parts = update.message().text().split(" ", 2);

        if (parts.length < 2 || (!parts[1].equals("instant") && !parts[1].equals("digest"))) {
            return new SendMessage(chatId, "Usage: /settings [instant|digest]");
        }

        String mode = parts[1];
        redisTemplate.opsForValue().set("notification-mode:" + chatId, mode);
        return new SendMessage(chatId, "Notification mode set to: " + mode);
    }
}
