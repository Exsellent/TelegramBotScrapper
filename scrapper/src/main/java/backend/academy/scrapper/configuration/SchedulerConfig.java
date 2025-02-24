package backend.academy.scrapper.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ApplicationConfig.class)
public class SchedulerConfig {

    private final ApplicationConfig applicationConfig;

    public SchedulerConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    @Bean
    public ApplicationConfig.Scheduler scheduler() {
        return applicationConfig.scheduler();
    }
}
