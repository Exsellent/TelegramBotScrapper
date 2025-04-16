package backend.academy.bot.configuration;

import backend.academy.bot.backoff.BackOffStrategy;
import backend.academy.bot.backoff.ConstBackOff;
import backend.academy.bot.backoff.ExponentialBackOff;
import backend.academy.bot.backoff.LinearBackOff;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BackOffProperties.class)
public class BackOffConfig {

    private final BackOffProperties backOffProperties;

    @Autowired
    public BackOffConfig(BackOffProperties backOffProperties) {
        this.backOffProperties = backOffProperties;
    }

    @Bean
    @ConditionalOnProperty(name = "backoff.strategy", havingValue = "constant")
    public BackOffStrategy constBackOffStrategy() {
        return new ConstBackOff(backOffProperties.getSettings().getInitialDelay());
    }

    @Bean
    @ConditionalOnProperty(name = "backoff.strategy", havingValue = "linear")
    public BackOffStrategy linearBackOffStrategy() {
        return new LinearBackOff(
                backOffProperties.getSettings().getInitialDelay(),
                backOffProperties.getSettings().getIncrement());
    }

    @Bean
    @ConditionalOnProperty(name = "backoff.strategy", havingValue = "exponential")
    public BackOffStrategy exponentialBackOffStrategy() {
        BackoffSettings settings = backOffProperties.getSettings();
        if (settings == null) {
            return new ExponentialBackOff(1000L, 2.0); // Значения по умолчанию
        }
        return new ExponentialBackOff(settings.getInitialDelay(), settings.getMultiplier());
    }
}
