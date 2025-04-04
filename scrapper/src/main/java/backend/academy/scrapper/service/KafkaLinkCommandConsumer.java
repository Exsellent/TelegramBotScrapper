package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.LinkDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class KafkaLinkCommandConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaLinkCommandConsumer.class);
    private final LinkService linkService;
    private final ChatLinkService chatLinkService;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaLinkCommandConsumer(
            LinkService linkService, ChatLinkService chatLinkService, ObjectMapper objectMapper) {
        this.linkService = linkService;
        this.chatLinkService = chatLinkService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.link-commands}", groupId = "scrapper-group")
    public void processCommand(String message, @Header(KafkaHeaders.RECEIVED_KEY) String chatIdStr) {
        Long chatId = Long.valueOf(chatIdStr);

        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String command = jsonNode.get("command").asText();
            String link = jsonNode.get("link").asText();
            Map<String, String> filters = objectMapper.convertValue(jsonNode.get("filters"), new TypeReference<>() {});

            if ("add".equalsIgnoreCase(command)) {
                LinkDTO linkDTO = linkService.add(link, "Added via Kafka");
                chatLinkService.addLinkToChat(chatId, linkDTO.getLinkId(), filters); // Передаём фильтры
                LOGGER.info("Added link {} for chat {}", link, chatId);
            } else if ("remove".equalsIgnoreCase(command)) {
                LinkDTO linkDTO = linkService.findByUrl(link);
                if (linkDTO != null) {
                    chatLinkService.removeLinkFromChat(chatId, linkDTO.getLinkId());
                    LOGGER.info("Removed link {} for chat {}", link, chatId);
                } else {
                    LOGGER.warn("Link {} not found for removal in chat {}", link, chatId);
                }
            } else {
                LOGGER.warn("Unknown command received: {}", command);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process Kafka message: {}", message, e);
        }
    }
}
