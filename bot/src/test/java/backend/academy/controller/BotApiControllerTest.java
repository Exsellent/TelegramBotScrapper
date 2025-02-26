package backend.academy.controller;

import backend.academy.bot.controller.BotApiController;
import backend.academy.bot.dto.LinkUpdateRequest;
import backend.academy.bot.exception.GlobalExceptionHandler;
import backend.academy.bot.insidebot.TelegramBotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class BotApiControllerTest {

    @Mock
    private TelegramBotService telegramBotService;

    @InjectMocks
    private BotApiController botApiController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(botApiController)
            .setControllerAdvice(new GlobalExceptionHandler()) // Добавлено
            .build();
    }

    @Test
    public void testPostUpdateWithEmptyUrl() throws Exception {
        LinkUpdateRequest request = new LinkUpdateRequest(
            1L,
            "",
            "Пример описания",
            "update",
            Arrays.asList(123L, 456L)
        );

        mockMvc.perform(post("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())  // Ожидаем 400 Bad Request
            .andExpect(result ->
                assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException,
                    "Expected MethodArgumentNotValidException but got " + result.getResolvedException())
            );

        verify(telegramBotService, never()).sendChatMessage(anyLong(), anyString());
    }

    @Test
    public void testPostUpdateWithNullUrl() throws Exception {
        LinkUpdateRequest request = new LinkUpdateRequest(
            1L,
            null,
            "Пример описания",
            "update",
            Arrays.asList(123L, 456L)
        );

        mockMvc.perform(post("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())  // Ожидаем 400 Bad Request
            .andExpect(result ->
                assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException,
                    "Expected MethodArgumentNotValidException but got " + result.getResolvedException())
            );

        verify(telegramBotService, never()).sendChatMessage(anyLong(), anyString());
    }
}
