package backend.academy.bot.command;

import backend.academy.bot.dto.LinkResponse;
import backend.academy.bot.service.KafkaService;
import backend.academy.bot.service.RedisCacheService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ListCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListCommand.class);
    private final KafkaService kafkaService;
    private final RedisCacheService redisCacheService;
    private final Map<Long, CompletableFuture<List<LinkResponse>>> pendingRequests = new ConcurrentHashMap<>();

    public Map<Long, CompletableFuture<List<LinkResponse>>> getPendingRequests() {
        return pendingRequests;
    }

    @Autowired
    public ListCommand(KafkaService kafkaService, RedisCacheService redisCacheService) {
        this.kafkaService = kafkaService;
        this.redisCacheService = redisCacheService;
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

        // Проверяем кэш
        List<LinkResponse> cachedLinks = redisCacheService.getLinks(chatId);
        if (cachedLinks != null && !cachedLinks.isEmpty()) {
            LOGGER.info("Returning cached links for chat {}", chatId);
            return buildResponse(chatId, cachedLinks);
        }

        // CompletableFuture для ожидания ответа
        CompletableFuture<List<LinkResponse>> future = new CompletableFuture<>();
        pendingRequests.put(chatId, future);

        try {
            // Отправляем запрос в Kafka
            kafkaService.sendCommand(chatId, "list", Collections.emptyMap());

            // Ожидаем ответа с таймаутом
            List<LinkResponse> links = future.get(5, TimeUnit.SECONDS);
            return buildResponse(chatId, links != null ? links : Collections.emptyList());
        } catch (TimeoutException e) {
            LOGGER.error("Timeout waiting for list response for chat {}", chatId);
            return new SendMessage(chatId, "Timeout while fetching links.");
        } catch (Exception e) {
            LOGGER.error("Error handling /list command", e);
            return new SendMessage(chatId, "An error occurred while fetching links.");
        } finally {
            pendingRequests.remove(chatId);
        }
    }

    private SendMessage buildResponse(Long chatId, List<LinkResponse> links) {
        if (links.isEmpty()) {
            return new SendMessage(chatId, "The list of tracked links is empty.");
        }
        StringBuilder messageBuilder = new StringBuilder("Tracked links:\n");
        for (LinkResponse link : links) {
            messageBuilder.append(link.getUrl()).append("\n");
        }
        redisCacheService.setLinks(chatId, links);
        return new SendMessage(chatId, messageBuilder.toString());
    }
}
