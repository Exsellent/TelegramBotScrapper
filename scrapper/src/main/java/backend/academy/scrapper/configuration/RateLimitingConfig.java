package backend.academy.scrapper.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limiting")
@Getter
@Setter
public class RateLimitingConfig {
    private int requestLimit;
    private int windowSeconds;
}
