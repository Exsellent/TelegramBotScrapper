package backend.academy.bot.configuration;

import com.pengrad.telegrambot.TelegramBot;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Slf4j
@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class ApplicationConfig {

    @Value("${APP_TELEGRAM_TOKEN}")
    private String telegramToken;

    @PostConstruct
    public void validateConfig() {
        log.info("Current working directory: {}", System.getProperty("user.dir"));
        log.info("Attempting to read token configuration...");

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
