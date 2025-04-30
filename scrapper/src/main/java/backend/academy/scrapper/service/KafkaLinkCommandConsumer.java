package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.LinkDTO;
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
    private static final String COMMAND_KEY = "command";
    private static final String LINK_KEY = "link";
    private static final String FILTERS_KEY = "filters";

    private final LinkService linkService;
    private final ChatLinkService chatLinkService;
    private final MessageParserService messageParserService;

    @Autowired
    public KafkaLinkCommandConsumer(
            LinkService linkService, ChatLinkService chatLinkService, MessageParserService messageParserService) {
        this.linkService = linkService;
        this.chatLinkService = chatLinkService;
        this.messageParserService = messageParserService;
    }

    @KafkaListener(topics = "${app.kafka.topics.link-commands}", groupId = "scrapper-group")
    public void processCommand(String message, @Header(KafkaHeaders.RECEIVED_KEY) String chatIdStr) {
        Long chatId = Long.valueOf(chatIdStr);

        try {
            Map<String, Object> data = messageParserService.parseMessage(message);
            String command = (String) data.get(COMMAND_KEY);
            String link = (String) data.get(LINK_KEY);
            Map<String, String> filters = (Map<String, String>) data.get(FILTERS_KEY);

            switch (command.toLowerCase()) {
                case "add":
                    LinkDTO linkDTO = linkService.add(link, "Added via Kafka");
                    chatLinkService.addLinkToChat(chatId, linkDTO.getLinkId(), filters);
                    LOGGER.info("Added link {} for chat {}", link, chatId);
                    break;
                case "remove":
                    LinkDTO existingLink = linkService.findByUrl(link);
                    if (existingLink != null) {
                        chatLinkService.removeLinkFromChat(chatId, existingLink.getLinkId());
                        LOGGER.info("Removed link {} for chat {}", link, chatId);
                    } else {
                        LOGGER.warn("Link {} not found for removal in chat {}", link, chatId);
                    }
                    break;
                default:
                    LOGGER.warn("Unknown command received: {}", command);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process Kafka message: {}", message, e);
        }
    }
}
