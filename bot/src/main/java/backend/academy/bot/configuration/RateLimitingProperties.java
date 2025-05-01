package backend.academy.bot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bot.rate-limiting")
public class RateLimitingProperties {
    private int capacity;
    private int tokens;
    private int refillDuration;
}
