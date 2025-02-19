package backend.academy.bot.configuration;

import backend.academy.bot.command.BotMenuInitializer;
import backend.academy.bot.command.Command;
import com.pengrad.telegrambot.TelegramBot;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommandConfig {

    @Bean
    public BotMenuInitializer botMenuInitializer(List<Command> commands, TelegramBot bot) {
        return new BotMenuInitializer(commands, bot);
    }
}
