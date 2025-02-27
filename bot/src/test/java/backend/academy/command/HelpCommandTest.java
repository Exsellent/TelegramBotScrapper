package backend.academy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.command.Command;
import backend.academy.bot.command.HelpCommand;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class HelpCommandTest {

    @Test
    void testHelpCommandResponse() {
        // Мокаем команду /start
        Command startCommandMock = mock(Command.class);
        when(startCommandMock.command()).thenReturn("/start");
        when(startCommandMock.description()).thenReturn("Launch the bot"); // Исправлено

        // Создаем список команд
        List<Command> commands = List.of(startCommandMock);

        // Инициализируем HelpCommand
        HelpCommand command = new HelpCommand(commands);
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        // Настраиваем моки
        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);

        // Выполняем handle
        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");

        // Формируем ожидаемый ответ
        String expectedResponse =
                """
        Available Commands:
        /start: Launch the bot

        Supported link formats:
        GitHub: https://github.com/example/repo
        StackOverflow: https://stackoverflow.com/questions/example
        """;

        assertEquals(expectedResponse.trim(), text.trim());
    }
}
