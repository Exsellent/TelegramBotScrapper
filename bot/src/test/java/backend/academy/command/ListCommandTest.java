package backend.academy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.command.ListCommand;
import backend.academy.bot.dto.LinkResponse;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

class ListCommandTest {

    @Mock
    private KafkaTemplate<String, List<LinkResponse>> kafkaTemplate;

    @Mock
    private RedisTemplate<String, List<LinkResponse>> redisTemplate;

    @Mock
    private ValueOperations<String, List<LinkResponse>> valueOperations;

    private ListCommand command;  // Без @InjectMocks

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        command = new ListCommand(kafkaTemplate, redisTemplate, "link-updates");
    }

    @Test
    void testListCommandWithEmptyCache() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
        when(redisTemplate.opsForValue().get("tracked-links:123")).thenReturn(null);

        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");

        assertEquals("Fetching links, please wait...", text);
        verify(kafkaTemplate).send(eq("link-updates"), eq("123"), eq(null));
    }

    @Test
    void testListCommandWithCachedLinks() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        LinkResponse link = new LinkResponse(1L, "https://example.com", "Description");
        List<LinkResponse> cachedLinks = Collections.singletonList(link);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
        when(redisTemplate.opsForValue().get("tracked-links:123")).thenReturn(cachedLinks);

        SendMessage response = command.handle(update);
        String text = (String) response.getParameters().get("text");

        assertEquals("Tracked links:\nhttps://example.com\n", text);
    }
}
