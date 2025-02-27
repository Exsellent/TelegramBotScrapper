package backend.academy.bot.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3) // 3 попытки на каждый запрос
                .fixedBackoff(1000) // Задержка 1 секунда между попытками
                .retryOn(Exception.class) // Повторяем только при исключениях
                .build();
    }
}
