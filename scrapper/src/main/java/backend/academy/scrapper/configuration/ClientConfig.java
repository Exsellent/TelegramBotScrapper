package backend.academy.scrapper.configuration;

import backend.academy.scrapper.client.github.GitHubClient;
import backend.academy.scrapper.client.github.GitHubClientImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class ClientConfig {

    @Bean
    public GitHubClient gitHubClient(
            @Qualifier("gitHubWebClient") WebClient gitHubWebClient,
            @Value("${github.retry.maxAttempts:3}") int maxAttempts,
            @Value("${github.retry.backoffSeconds:2}") long backoffSeconds,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        return new GitHubClientImpl(gitHubWebClient, maxAttempts, backoffSeconds, circuitBreakerRegistry);
    }

    @Bean("gitHubWebClient")
    public WebClient gitHubWebClient(
            WebClient.Builder webClientBuilder,
            @Value("${github.base.url}") String gitHubBaseUrl,
            @Value("${http.client.connectTimeout:5000}") int connectTimeout,
            @Value("${http.client.readTimeout:10000}") int readTimeout) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS)));
        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(gitHubBaseUrl)
                .build();
    }

    @Bean
    public WebClient stackOverflowWebClient(
            WebClient.Builder webClientBuilder,
            @Value("${stackoverflow.base.url}") String stackOverflowBaseUrl,
            @Value("${http.client.connectTimeout:5000}") int connectTimeout,
            @Value("${http.client.readTimeout:10000}") int readTimeout) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS)));
        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(stackOverflowBaseUrl)
                .build();
    }

    @Bean
    public WebClient botWebClient(
            WebClient.Builder webClientBuilder,
            @Value("${bot.api.baseurl}") String botBaseUrl,
            @Value("${http.client.connectTimeout:5000}") int connectTimeout,
            @Value("${http.client.readTimeout:10000}") int readTimeout) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS)));
        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(botBaseUrl)
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
