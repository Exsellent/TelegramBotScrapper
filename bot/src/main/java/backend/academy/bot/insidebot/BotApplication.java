package backend.academy.bot.insidebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@ComponentScan(basePackages = {"backend.academy.bot"})
public class BotApplication {
    public static void main(String[] args) {

        SpringApplication.run(BotApplication.class, args);
    }
}
