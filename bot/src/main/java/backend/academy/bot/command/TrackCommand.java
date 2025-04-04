package backend.academy.bot.command;

import backend.academy.bot.dto.LinkResponse;
import backend.academy.bot.exception.FilterValidationException;
import backend.academy.bot.exception.InvalidLinkException;
import backend.academy.bot.utils.LinkParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TrackCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackCommand.class);
    private final ConversationManager conversationManager;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisTemplate<String, List<LinkResponse>> redisTemplate;
    private final ObjectMapper objectMapper;
    private final String linkCommandsTopic;

    @Autowired
    public TrackCommand(
            ConversationManager conversationManager,
            KafkaTemplate<String, String> kafkaTemplate,
            RedisTemplate<String, List<LinkResponse>> redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.link-commands}") String linkCommandsTopic) {
        this.conversationManager = conversationManager;
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.linkCommandsTopic = linkCommandsTopic;
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
                    Map<String, String> effectiveFilters = data.getFilters() != null ? data.getFilters() : Map.of();
                    String jsonMessage = objectMapper.writeValueAsString(
                            Map.of("command", "add", "link", data.getUrl(), "filters", effectiveFilters));
                    kafkaTemplate.send(linkCommandsTopic, chatId.toString(), jsonMessage);
                    redisTemplate.delete("tracked-links:" + chatId);
                    conversationManager.setUserState(chatId, ConversationState.IDLE);
                    conversationManager.clearTrackingData(chatId);
                    LOGGER.info("Sent link tracking request for chat {}: {}", chatId, data.getUrl());
                    return new SendMessage(chatId, "Link tracking request sent successfully!");
                } catch (Exception e) {
                    LOGGER.error("Error sending link tracking request for chat {}", chatId, e);
                    return new SendMessage(chatId, "Error sending link tracking request. Please try again.");
                }

            default:
                return new SendMessage(chatId, "Unknown state. Please start over with /track");
        }
        return new SendMessage(chatId, "Please continue the tracking process.");
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
