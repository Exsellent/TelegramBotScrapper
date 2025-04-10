package backend.academy.bot.configuration;

import com.pengrad.telegrambot.TelegramBot;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ApplicationConfig {

    @Value("${app.telegram-token}")
    private String telegramToken;

    @PostConstruct
    public void validateConfig() {
        log.info("Current working directory: {}", System.getProperty("user.dir"));
        log.info("Attempting to read token configuration...");
        log.info("Telegram token read: {}", telegramToken);
        if (telegramToken == null || telegramToken.trim().isEmpty()) {
            String errorMessage = "Telegram token not configured - please ensure APP_TELEGRAM_TOKEN is set";
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        log.info("Telegram token successfully loaded");
    }

    @Bean
    public TelegramBot telegramBot() {
        log.info("Creating TelegramBot instance");
        TelegramBot bot = new TelegramBot(telegramToken.trim());
        log.info("TelegramBot instance created successfully");
        return bot;
    }
}
