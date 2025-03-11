package backend.academy.scrapper.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.scrapper.dto.AddLinkRequest;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.dto.RemoveLinkRequest;
import backend.academy.scrapper.service.ChatLinkService;
import backend.academy.scrapper.service.ChatService;
import backend.academy.scrapper.service.LinkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
public class ScrapperApiControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private LinkService linkService;

    @Mock
    private ChatLinkService chatLinkService;

    @InjectMocks
    private ScrapperApiController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private final Long testChatId = 1L;
    private final Long testLinkId = 1L;
    private final String testUrl = "http://example.com";
    private final String testDescription = "Test Description";
    private LinkDTO testLink;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(jsonConverter, stringConverter)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();

        testLink = new LinkDTO(testLinkId, testUrl, testDescription, LocalDateTime.now(), null, null);
    }

    @Test
    public void registerChat_ShouldReturnSuccessMessage() throws Exception {
        String expectedMessage = "Chat " + testChatId + " successfully registered.";
        mockMvc.perform(post("/api/tg-chat/{id}", testChatId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // Исправлено на JSON
                .andExpect(content().string("\"" + expectedMessage + "\"")); // JSON-строка с кавычками

        verify(chatService, times(1)).register(testChatId);
    }

    @Test
    public void deleteChat_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/tg-chat/{id}", testChatId)).andExpect(status().isNoContent());

        verify(chatService, times(1)).unregister(testChatId);
    }

    @Test
    public void getAllLinks_ShouldReturnListLinksResponse() throws Exception {
        when(chatLinkService.findAllLinksForChat(testChatId))
                .thenReturn(List.of(new ChatLinkDTO(testChatId, testLinkId, LocalDateTime.now())));
        when(linkService.findById(testLinkId)).thenReturn(testLink);

        mockMvc.perform(get("/api/links").header("Tg-Chat-Id", testChatId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.links[0].id").value(testLinkId))
                .andExpect(jsonPath("$.links[0].url").value(testUrl))
                .andExpect(jsonPath("$.links[0].description").value(testDescription))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    public void addLink_ShouldReturnLinkResponse() throws Exception {
        when(linkService.add(testUrl, testDescription)).thenReturn(testLink);

        AddLinkRequest addLinkRequest = new AddLinkRequest(testUrl, testDescription);

        mockMvc.perform(post("/api/links")
                        .header("Tg-Chat-Id", testChatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addLinkRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testLinkId))
                .andExpect(jsonPath("$.url").value(testUrl))
                .andExpect(jsonPath("$.description").value(testDescription));

        verify(chatLinkService, times(1)).addLinkToChat(testChatId, testLinkId);
    }

    @Test
    public void removeLink_ShouldReturnLinkResponse() throws Exception {
        when(linkService.findByUrl(testUrl)).thenReturn(testLink);
        when(chatLinkService.existsChatsForLink(testLinkId)).thenReturn(false);

        // Создание RemoveLinkRequest вручную
        RemoveLinkRequest removeLinkRequest = new RemoveLinkRequest(testUrl);

        mockMvc.perform(delete("/api/links")
                        .header("Tg-Chat-Id", testChatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(removeLinkRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testLinkId))
                .andExpect(jsonPath("$.url").value(testUrl))
                .andExpect(jsonPath("$.description").value(testDescription));

        verify(chatLinkService, times(1)).removeLinkFromChat(testChatId, testLinkId);
        verify(linkService, times(1)).remove(testUrl);
    }
}
