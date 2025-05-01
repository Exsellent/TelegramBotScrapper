package backend.academy.bot.configuration;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.backoff")
public class BackOffProperties {
    private BackoffSettings settings;
    private List<Integer> retryableStatusCodes;
}
