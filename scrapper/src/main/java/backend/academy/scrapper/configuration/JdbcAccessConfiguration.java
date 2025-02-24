package backend.academy.scrapper.configuration;

import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.dao.ChatLinkDao;
import backend.academy.scrapper.dao.LinkDao;
import backend.academy.scrapper.database.jpa.jdbc.service.JdbcChatLinkService;
import backend.academy.scrapper.database.jpa.jdbc.service.JdbcChatService;
import backend.academy.scrapper.database.jpa.jdbc.service.JdbcLinkService;
import backend.academy.scrapper.service.ChatLinkService;
import backend.academy.scrapper.service.ChatService;
import backend.academy.scrapper.service.LinkService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app", name = "database-access-type", havingValue = "jdbc")
public class JdbcAccessConfiguration {

    @Bean
    public LinkService linkService(LinkDao linkDao) {
        return new JdbcLinkService(linkDao);
    }

    @Bean
    public ChatService chatService(ChatDao chatDao) {
        return new JdbcChatService(chatDao);
    }

    @Bean
    public ChatLinkService chatLinkService(ChatLinkDao chatLinkDao, ChatDao chatDao) {
        return new JdbcChatLinkService(chatLinkDao, chatDao);
    }
}
