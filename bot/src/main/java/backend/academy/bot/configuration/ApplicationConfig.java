package backend.academy.bot.configuration;

import com.pengrad.telegrambot.TelegramBot;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "app")
public class ApplicationConfig {

    @NotEmpty(message = "Telegram token must not be empty")
    private String telegramToken;

    @Bean
    public TelegramBot telegramBot() {
        return new TelegramBot(telegramToken);
    }

    public void setTelegramToken(String telegramToken) {
        this.telegramToken = telegramToken.trim();
    }
}
