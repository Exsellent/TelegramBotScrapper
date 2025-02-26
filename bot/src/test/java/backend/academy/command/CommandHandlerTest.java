package backend.academy.command;

import backend.academy.bot.command.CommandHandler;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

public class CommandHandlerTest {

    @Test
    public void testUnknownCommand() {
        // Arrange: Подготовка mock-объектов
        Update update = Mockito.mock(Update.class);
        Message message = Mockito.mock(Message.class);
        Chat chat = Mockito.mock(Chat.class);

        // Настройка поведения mock-объектов
        when(update.message()).thenReturn(message);
        when(message.text()).thenReturn("/unknown"); // Неизвестная команда
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L); // Пример ID чата

        // Создаем CommandHandler с пустым списком команд
        CommandHandler commandHandler = new CommandHandler(Collections.emptyList());

        // Act: Вызов метода handleUpdate
        SendMessage response = commandHandler.handleUpdate(update);

        // Assert: Проверка результата
        assertNotNull(response, "Ответ не должен быть null");
        assertEquals(123L, response.getParameters().get("chat_id"), "ID чата должен совпадать");
        assertEquals("Unknown command. Use /help to view the list of available commands.", response.getParameters().get("text"), "Текст сообщения должен быть корректным");
    }
}
