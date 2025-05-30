package backend.academy.scrapper.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.dto.LinkDTO;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KafkaLinkCommandConsumerTest {

    private KafkaLinkCommandConsumer consumer;
    private LinkService linkService;
    private ChatLinkService chatLinkService;
    private MessageParserService messageParserService;

    @BeforeEach
    void setup() {
        linkService = mock(LinkService.class);
        chatLinkService = mock(ChatLinkService.class);
        messageParserService = mock(MessageParserService.class);
        consumer = new KafkaLinkCommandConsumer(linkService, chatLinkService, messageParserService);
    }

    @Test
    void testAddCommand() throws Exception {
        String message = "{\"command\": \"add\", \"link\": \"https://example.com\", \"filters\": {\"tag\": \"test\"}}";
        String chatId = "123";

        LinkDTO linkDTO = LinkDTO.builder()
                .linkId(1L)
                .url("https://example.com")
                .description("Added via Kafka")
                .build();

        when(messageParserService.parseMessage(message))
                .thenReturn(Map.of("command", "add", "link", "https://example.com", "filters", Map.of("tag", "test")));
        when(linkService.add("https://example.com", "Added via Kafka")).thenReturn(linkDTO);

        consumer.processCommand(message, chatId);

        verify(linkService).add("https://example.com", "Added via Kafka");
        verify(chatLinkService).addLinkToChat(123L, 1L, Map.of("tag", "test"));
    }

    @Test
    void testRemoveCommand() throws Exception {
        String message = "{\"command\": \"remove\", \"link\": \"https://example.com\", \"filters\": {}}";
        String chatId = "123";

        LinkDTO linkDTO =
                LinkDTO.builder().linkId(1L).url("https://example.com").build();

        when(messageParserService.parseMessage(message))
                .thenReturn(Map.of("command", "remove", "link", "https://example.com", "filters", Map.of()));
        when(linkService.findByUrl("https://example.com")).thenReturn(linkDTO);

        consumer.processCommand(message, chatId);

        verify(linkService).findByUrl("https://example.com");
        verify(chatLinkService).removeLinkFromChat(123L, 1L);
    }

    @Test
    void testRemoveCommandLinkNotFound() throws Exception {
        String message = "{\"command\": \"remove\", \"link\": \"https://example.com\", \"filters\": {}}";
        String chatId = "123";

        when(messageParserService.parseMessage(message))
                .thenReturn(Map.of("command", "remove", "link", "https://example.com", "filters", Map.of()));
        when(linkService.findByUrl("https://example.com")).thenReturn(null);

        consumer.processCommand(message, chatId);

        verify(linkService).findByUrl("https://example.com");
        verify(chatLinkService, never()).removeLinkFromChat(anyLong(), anyLong());
    }

    @Test
    void testInvalidJson() throws Exception {
        String message = "invalid json";
        String chatId = "123";

        when(messageParserService.parseMessage(message)).thenThrow(new RuntimeException("Invalid JSON"));

        consumer.processCommand(message, chatId);

        verify(linkService, never()).add(any(), any());
        verify(chatLinkService, never()).addLinkToChat(anyLong(), anyLong(), any());
    }
}
