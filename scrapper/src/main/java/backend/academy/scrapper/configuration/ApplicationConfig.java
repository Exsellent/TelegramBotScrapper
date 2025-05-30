package backend.academy.scrapper.configuration;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record ApplicationConfig(
        @NotNull Scheduler scheduler,
        AccessType databaseAccessType,
        String migrationsDir,
        @NotNull Integer checkIntervalMinutes,
        @NotNull KafkaConfig kafka,
        String telegramToken,
        String messageTransport) {
    public record Scheduler(boolean enable, @NotNull Duration interval, @NotNull Duration forceCheckDelay) {}

    public record KafkaConfig(@NotNull Topics topics) {
        public record Topics(@NotNull String notifications, @NotNull String dlq, @NotNull String linkCommands) {}
    }
}
