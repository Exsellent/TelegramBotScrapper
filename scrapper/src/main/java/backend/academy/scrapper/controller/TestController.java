package backend.academy.scrapper.controller;

import backend.academy.scrapper.exception.ChatNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/illegal-argument")
    public void throwIllegalArgument() {
        throw new IllegalArgumentException("Invalid input");
    }

    @GetMapping("/chat-not-found")
    public void throwChatNotFound() {
        throw new ChatNotFoundException("Chat not found");
    }
}
