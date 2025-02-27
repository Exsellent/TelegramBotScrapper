package backend.academy.scrapper.controller;

import backend.academy.scrapper.dto.AddLinkRequest;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.dto.LinkResponse;
import backend.academy.scrapper.dto.LinkUpdateRequest;
import backend.academy.scrapper.dto.ListLinksResponse;
import backend.academy.scrapper.dto.RemoveLinkRequest;
import backend.academy.scrapper.service.ChatLinkService;
import backend.academy.scrapper.service.ChatService;
import backend.academy.scrapper.service.LinkService;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class ScrapperApiController {
    private final ChatService chatService;
    private final LinkService linkService;
    private final ChatLinkService chatLinkService;
    private final WebClient gitHubWebClient;

    @PostMapping("/tg-chat/{id}")
    public ResponseEntity<String> registerChat(@PathVariable("id") Long id) {
        chatService.register(id);
        return ResponseEntity.ok("Chat " + id + " successfully registered.");
    }

    @DeleteMapping("/tg-chat/{id}")
    public ResponseEntity<Void> deleteChat(@PathVariable("id") Long id) {
        chatService.unregister(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/links", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListLinksResponse> getAllLinks(@RequestHeader("Tg-Chat-Id") Long tgChatId) {
        Collection<ChatLinkDTO> chatLinks = chatLinkService.findAllLinksForChat(tgChatId);
        List<LinkResponse> links = chatLinks.stream()
            .map(chatLink -> {
                LinkDTO link = linkService.findById(chatLink.getLinkId());
                return new LinkResponse(link.getLinkId(), link.getUrl(), link.getDescription());
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(new ListLinksResponse(links, links.size()));
    }

    @PostMapping(
        value = "/links",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LinkResponse> addLink(
        @RequestHeader("Tg-Chat-Id") Long tgChatId, @RequestBody @Valid AddLinkRequest addLinkRequest) {
        LinkDTO addedLink = linkService.add(addLinkRequest.getLink(), addLinkRequest.getDescription());
        chatLinkService.addLinkToChat(tgChatId, addedLink.getLinkId());
        return ResponseEntity.ok(
            new LinkResponse(addedLink.getLinkId(), addedLink.getUrl(), addedLink.getDescription()));
    }

    @DeleteMapping("/links")
    public ResponseEntity<LinkResponse> removeLink(
        @RequestHeader("Tg-Chat-Id") Long tgChatId, @RequestBody @Valid RemoveLinkRequest removeLinkRequest) {
        LinkDTO link = linkService.findByUrl(removeLinkRequest.getLink());
        chatLinkService.removeLinkFromChat(tgChatId, link.getLinkId());
        if (!chatLinkService.existsChatsForLink(link.getLinkId())) {
            linkService.remove(removeLinkRequest.getLink());
        }
        return ResponseEntity.ok(new LinkResponse(link.getLinkId(), link.getUrl(), link.getDescription()));
    }

    @GetMapping("/updates")
    public ResponseEntity<List<LinkUpdateRequest>> getUpdates() {
        try {
            List<LinkUpdateRequest> updates = List.of(); // Пустой список, так как логика не реализована
            return ResponseEntity.ok(updates);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/updates")
    public ResponseEntity<Void> receiveUpdates(@RequestBody List<LinkUpdateRequest> requests) {
        try {
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    public void sendUpdate(LinkUpdateRequest update) {
        gitHubWebClient
            .post()
            .uri("http://localhost:8080/api/updates")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(update)
            .retrieve()
            .toBodilessEntity()
            .block();
    }
}
