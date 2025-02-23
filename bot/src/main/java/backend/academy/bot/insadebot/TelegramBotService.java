package backend.academy.bot.insadebot;

import backend.academy.bot.command.CommandHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TelegramBotService implements Bot {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramBotService.class);
    private final TelegramBot bot;
    private final CommandHandler commandHandler;
    private volatile boolean isRunning = false;

    public TelegramBotService(CommandHandler commandHandler, TelegramBot bot) {
        this.commandHandler = commandHandler;
        this.bot = bot;
    }

    @PostConstruct
    public void init() {
        start();
    }

    @Override
    public void start() {
        if (!isRunning) {
            isRunning = true;
            bot.setUpdatesListener(this, e -> {
                if (e != null) {
                    LOGGER.error("Error in updates listener", e);
                }
            });
        }
    }

    @Override
    public int process(List<Update> updates) {
        if (updates == null || updates.isEmpty()) {
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }
        for (Update update : updates) {
            try {
                processUpdate(update);
            } catch (Exception e) {
                LOGGER.error("Error processing update: " + update.updateId(), e);
            }
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void processUpdate(Update update) {
        if (update == null) {
            return;
        }
        try {
            BaseRequest<?, ?> response = commandHandler.handleUpdate(update);
            if (response != null) {
                execute(response);
            }
        } catch (Exception e) {
            LOGGER.error("Update processing error for update ID: " + update.updateId(), e);
        }
    }

    @Override
    public <T extends BaseRequest<T, R>, R extends BaseResponse> R execute(BaseRequest<T, R> request) {
        try {
            return bot.execute(request);
        } catch (Exception e) {
            LOGGER.error("Error executing request: {}", e.getMessage());
            throw new RuntimeException("Failed to execute request", e);
        }
    }

    @Override
    @PreDestroy
    public void close() {
        if (isRunning) {
            isRunning = false;
            try {
                bot.removeGetUpdatesListener();
                bot.shutdown();
                LOGGER.info("Bot has been successfully shut down.");
            } catch (Exception e) {
                LOGGER.error("Error during bot shutdown", e);
            }
        }
    }

    public void sendChatMessage(Long chatId, String messageText) {
        try {
            SendMessage sendMessageRequest = new SendMessage(chatId, messageText);
            SendResponse response = bot.execute(sendMessageRequest);

            if (response.isOk()) {
                LOGGER.info("Message sent successfully to chat ID: {}", chatId);
            } else {
                LOGGER.error("Failed to send message to chat ID: {}. Error: {}", chatId, response.description());
            }
        } catch (Exception e) {
            LOGGER.error("Error sending message to chat ID: {}. Error: {}", chatId, e.getMessage());
        }
    }
}
