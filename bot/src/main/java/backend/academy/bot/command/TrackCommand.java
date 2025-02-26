package backend.academy.bot.command;

import backend.academy.bot.client.ScrapperApiClient;
import backend.academy.bot.dto.AddLinkRequest;
import backend.academy.bot.exception.FilterValidationException;
import backend.academy.bot.exception.InvalidLinkException;
import backend.academy.bot.utils.LinkParser;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TrackCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackCommand.class);
    private final ScrapperApiClient scrapperApiClient;
    private final ConversationManager conversationManager;

    @Autowired
    public TrackCommand(ScrapperApiClient scrapperApiClient, ConversationManager conversationManager) {
        this.scrapperApiClient = scrapperApiClient;
        this.conversationManager = conversationManager;
    }

    @Override
    public String command() {
        return "/track";
    }

    @Override
    public String description() {
        return "Start tracking the link";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        String messageText = update.message().text();
        ConversationState state = conversationManager.getUserState(chatId);

        // Инициализация данных для отслеживания
        ConversationManager.TrackingData data = conversationManager.getTrackingData(chatId);

        switch (state) {
            case IDLE:
                if (messageText.equals("/track")) {
                    conversationManager.setUserState(chatId, ConversationState.AWAITING_URL);
                    return new SendMessage(chatId, "Please enter the URL you want to track:");
                }
                break;

            case AWAITING_URL:
                if (!LinkParser.isValidURL(messageText)) {
                    throw new InvalidLinkException("Invalid URL format. Please enter a valid URL:");
                }
                data.setUrl(messageText);
                conversationManager.setUserState(chatId, ConversationState.AWAITING_TAGS);
                return new SendMessage(chatId, "Enter tags for this link (optional, space-separated) or type 'skip':");

            case AWAITING_TAGS:
                if (messageText.equals("skip")) {
                    break;
                }
                List<String> tags = Arrays.asList(messageText.split("\\s+"));
                data.setTags(tags);
                conversationManager.setUserState(chatId, ConversationState.AWAITING_FILTERS);
                return new SendMessage(
                        chatId, "Enter filters (format: key:value, e.g., 'user:john type:comment') or type 'skip':");

            case AWAITING_FILTERS:
                if (messageText.equals("skip")) {
                    break;
                }
                Map<String, String> filters = parseFilters(messageText);
                if (filters.isEmpty()) {
                    throw new FilterValidationException(
                            "Invalid filter format. Please enter filters in the correct format.");
                }
                data.setFilters(filters);

                try {
                    AddLinkRequest request = new AddLinkRequest(data.getUrl(), data.getTags(), data.getFilters());
                    scrapperApiClient.addLink(chatId, request);
                    conversationManager.setUserState(chatId, ConversationState.IDLE);
                    conversationManager.clearTrackingData(chatId);
                    return new SendMessage(chatId, "Link successfully added with specified tags and filters!");
                } catch (Exception e) {
                    LOGGER.error("Error adding link", e);
                    return new SendMessage(chatId, "Error adding link. Please try again.");
                }

            default:
                return new SendMessage(chatId, "Unknown state. Please start over with /track");
        }

        return null;
    }

    private Map<String, String> parseFilters(String filterText) {
        Map<String, String> filters = new HashMap<>();
        String[] filterPairs = filterText.split("\\s+");

        for (String pair : filterPairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                filters.put(keyValue[0], keyValue[1]);
            }
        }

        return filters;
    }
}
