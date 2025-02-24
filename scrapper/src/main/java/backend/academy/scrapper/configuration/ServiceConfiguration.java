package backend.academy.scrapper.configuration;

import backend.academy.scrapper.client.github.GitHubClient;
import backend.academy.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.scrapper.repository.repository.InMemoryChatRepository;
import backend.academy.scrapper.repository.repository.InMemoryChatRepositoryImpl;
import backend.academy.scrapper.service.ChatService;
import backend.academy.scrapper.service.ChatServiceImpl;
import backend.academy.scrapper.service.GitHubService;
import backend.academy.scrapper.service.StackOverflowService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(name = "app.database-access-type", havingValue = "in-memory")
public class ServiceConfiguration {

    @Bean
    public InMemoryChatRepository inMemoryChatRepository() {
        return new InMemoryChatRepositoryImpl();
    }

    @Bean
    @Primary
    public ChatService chatService(InMemoryChatRepository repository) {
        return new ChatServiceImpl(repository);
    }

    @Bean
    public GitHubService gitHubService(GitHubClient gitHubClient, ChatService chatService) {
        return new GitHubService(gitHubClient, chatService);
    }

    @Bean
    public StackOverflowService stackOverflowService(StackOverflowClient stackOverflowClient, ChatService chatService) {
        return new StackOverflowService(stackOverflowClient, chatService);
    }
}
