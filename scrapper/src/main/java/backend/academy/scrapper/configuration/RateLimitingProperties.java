package backend.academy.scrapper.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "scrapper.rate-limiting")
@Getter
@Setter
public class RateLimitingProperties {
    private int capacity;
    private int tokens;
    private int refillDuration;
}
