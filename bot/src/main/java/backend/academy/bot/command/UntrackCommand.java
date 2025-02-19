package backend.academy.bot.command;

import backend.academy.bot.client.ScrapperApiClient;
import backend.academy.bot.dto.RemoveLinkRequest;
import backend.academy.bot.exception.ChatNotFoundException;
import backend.academy.bot.exception.LinkNotFoundException;
import backend.academy.bot.utils.LinkParser;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UntrackCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(UntrackCommand.class);
    private final ScrapperApiClient scrapperApiClient;

    @Autowired
    public UntrackCommand(ScrapperApiClient scrapperApiClient) {
        this.scrapperApiClient = scrapperApiClient;
    }

    @Override
    public String command() {
        return "/untrack";
    }

    @Override
    public String description() {
        return "Stop tracking the link";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        String[] parts = update.message().text().split(" ", 2);

        if (parts.length < 2 || !LinkParser.isValidURL(parts[1])) {
            return new SendMessage(chatId, "Enter the correct URL to delete.");
        }

        try {
            scrapperApiClient.removeLink(chatId, new RemoveLinkRequest(parts[1]));
            LOGGER.info("Link removed from tracking: {}", parts[1]);
            return new SendMessage(chatId, "Link removed from tracking: " + parts[1]);
        } catch (LinkNotFoundException e) {
            return new SendMessage(chatId, "This link is not tracked.");
        } catch (ChatNotFoundException e) {
            return new SendMessage(chatId, "First, register using /start.");
        } catch (Exception e) {
            LOGGER.error("Error when deleting a link", e);
            return new SendMessage(chatId, "Error when deleting a link.");
        }
    }
}
