package backend.academy.bot.command;

import backend.academy.bot.client.ScrapperApiClient;
import backend.academy.bot.exception.ChatAlreadyRegisteredException;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StartCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartCommand.class);
    private final ScrapperApiClient scrapperApiClient;

    @Autowired
    public StartCommand(ScrapperApiClient scrapperApiClient) {
        this.scrapperApiClient = scrapperApiClient;
    }

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public String description() {
        return "Register";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        try {
            scrapperApiClient.registerChat(chatId);
            LOGGER.info("/start: chat {} successfully registered.", chatId);
            return new SendMessage(chatId, "Welcome!");
        } catch (ChatAlreadyRegisteredException e) {
            LOGGER.info("/start: the chat {} has already been registered.", chatId);
            return new SendMessage(chatId, "The chat has already been registered.");
        } catch (Exception e) {
            LOGGER.error("/start: error registering the chat {}.", chatId, e);
            return new SendMessage(chatId, "Error during registration. Try again later.");
        }
    }
}
