package backend.academy.scrapper.configuration;

import backend.academy.scrapper.database.jpa.service.JpaChatLinkService;
import backend.academy.scrapper.database.jpa.service.JpaChatService;
import backend.academy.scrapper.database.jpa.service.JpaLinkService;
import backend.academy.scrapper.repository.repository.ChatLinkRepository;
import backend.academy.scrapper.repository.repository.ChatRepository;
import backend.academy.scrapper.repository.repository.LinkRepository;
import backend.academy.scrapper.service.ChatLinkService;
import backend.academy.scrapper.service.ChatService;
import backend.academy.scrapper.service.LinkService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app", name = "database-access-type", havingValue = "jpa")
public class JpaAccessConfiguration {

    @Bean
    public LinkService linkService(LinkRepository linkRepository) {
        return new JpaLinkService(linkRepository);
    }

    @Bean
    public ChatService chatService(ChatRepository chatRepository) {
        return new JpaChatService(chatRepository);
    }

    @Bean
    public ChatLinkService chatLinkService(ChatLinkRepository chatLinkRepository, ChatRepository chatRepository) {
        return new JpaChatLinkService(chatLinkRepository, chatRepository);
    }
}
