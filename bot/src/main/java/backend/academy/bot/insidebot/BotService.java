package backend.academy.bot.insidebot;

import backend.academy.bot.configuration.BackoffSettings;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "bot")
@Validated
@Getter
@Setter
public class BotService {
    @NotNull
    private RateLimiting rateLimiting = new RateLimiting();

    @NotNull
    private BackoffSettings backoff = new BackoffSettings();

    @Getter
    @Setter
    public static class RateLimiting {
        private int capacity = 20;
        private int tokens = 20;
        private int refillDuration = 60;
    }
}
