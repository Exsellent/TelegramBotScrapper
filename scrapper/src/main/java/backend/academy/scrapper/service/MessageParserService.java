package backend.academy.scrapper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageParserService {
    private final ObjectMapper objectMapper;

    @Autowired
    public MessageParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> parseMessage(String message) throws Exception {
        return objectMapper.readValue(
                message, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
    }
}
