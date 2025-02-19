package backend.academy.bot.command;

import backend.academy.bot.client.ScrapperApiClient;
import backend.academy.bot.dto.LinkResponse;
import backend.academy.bot.dto.ListLinksResponse;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ListCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListCommand.class);
    private final ScrapperApiClient scrapperApiClient;

    @Autowired
    public ListCommand(ScrapperApiClient scrapperApiClient) {
        this.scrapperApiClient = scrapperApiClient;
    }

    @Override
    public String command() {
        return "/list";
    }

    @Override
    public String description() {
        return "Show a list of tracked links";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        LOGGER.info("Handling /list command for chat {}", chatId);

        try {
            ListLinksResponse response = scrapperApiClient.getAllLinks(chatId);

            if (response.getLinks().isEmpty()) {
                return new SendMessage(chatId, "The list of tracked links is empty.");
            }

            StringBuilder messageBuilder = new StringBuilder("Tracked links:\n");
            for (LinkResponse link : response.getLinks()) {
                messageBuilder.append(link.getUrl()).append("\n");
            }

            return new SendMessage(chatId, messageBuilder.toString());
        } catch (Exception e) {
            LOGGER.error("Error when getting the list of tracked links", e);
            return new SendMessage(chatId, "An error occurred while receiving the list of links.");
        }
    }
}
