package backend.academy.bot.service;

import backend.academy.bot.command.Command;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;

public interface UserMessageProcessor {
    List<Command> getCommands();

    SendMessage process(Update update);
}
