package backend.academy.scrapper.configuration;

import backend.academy.scrapper.client.BotApiClient;
import backend.academy.scrapper.client.github.GitHubClient;
import backend.academy.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.dao.ChatLinkDao;
import backend.academy.scrapper.dao.LinkDao;
import backend.academy.scrapper.database.jdbc.service.JdbcChatLinkService;
import backend.academy.scrapper.database.jdbc.service.JdbcChatService;
import backend.academy.scrapper.database.jdbc.service.SqlLinkService; // Обновлено
import backend.academy.scrapper.database.scheduler.LinkUpdaterScheduler;
import backend.academy.scrapper.service.ChatLinkService;
import backend.academy.scrapper.service.ChatService;
import backend.academy.scrapper.service.GitHubService;
import backend.academy.scrapper.service.LinkService;
import backend.academy.scrapper.service.StackOverflowService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app", name = "database-access-type", havingValue = "JDBC")
public class JdbcAccessConfiguration {
    @Bean
    public LinkService linkService(LinkDao linkDao) {
        return new SqlLinkService(linkDao);
    }

    @Bean
    public ChatService chatService(ChatDao chatDao) {
        return new JdbcChatService(chatDao);
    }

    @Bean
    public ChatLinkService chatLinkService(ChatLinkDao chatLinkDao, ChatDao chatDao) {
        return new JdbcChatLinkService(chatLinkDao, chatDao);
    }

    @Bean
    public GitHubService gitHubService(GitHubClient gitHubClient, ChatService chatService) {
        return new GitHubService(gitHubClient, chatService);
    }

    @Bean
    public StackOverflowService stackOverflowService(StackOverflowClient stackOverflowClient, ChatService chatService) {
        return new StackOverflowService(stackOverflowClient, chatService);
    }

    @Bean
    public LinkUpdaterScheduler linkUpdaterScheduler(
            LinkService linkService,
            ChatLinkService chatLinkService,
            GitHubService gitHubService,
            StackOverflowService stackOverflowService,
            BotApiClient botApiClient,
            @Value("${app.check-interval-minutes}") int checkIntervalMinutes) {
        return new LinkUpdaterScheduler(
                linkService, chatLinkService, gitHubService, stackOverflowService, botApiClient, checkIntervalMinutes);
    }
}
