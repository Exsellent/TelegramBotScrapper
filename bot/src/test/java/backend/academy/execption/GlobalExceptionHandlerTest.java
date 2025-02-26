package backend.academy.execption;

import backend.academy.bot.dto.LinkUpdateRequest;
import backend.academy.bot.insidebot.BotApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BotApplication.class)
@AutoConfigureMockMvc
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void postUpdateWithInvalidDataShouldReturnBadRequest() throws Exception {
        LinkUpdateRequest request = new LinkUpdateRequest();
        request.setUrl(""); // Явно задаем пустой URL

        mockMvc.perform(post("/updates").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.exceptionMessage").value("URL cannot be empty"));
    }
}
