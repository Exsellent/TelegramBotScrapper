package backend.academy.scrapper.database.scheduler;

import backend.academy.scrapper.client.BotApiClient;
import backend.academy.scrapper.configuration.ApplicationConfig;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.dto.Comment;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.dto.LinkUpdateRequest;
import backend.academy.scrapper.dto.QuestionResponse;
import backend.academy.scrapper.service.ChatLinkService;
import backend.academy.scrapper.service.GitHubService;
import backend.academy.scrapper.service.LinkService;
import backend.academy.scrapper.service.StackOverflowService;
import backend.academy.scrapper.utils.GitHubLinkExtractor;
import backend.academy.scrapper.utils.StackOverflowLinkExtractor;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@SuppressWarnings("MultipleStringLiterals")
public class LinkUpdaterScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkUpdaterScheduler.class);
    private static final int CHECK_LINK_AFTER = 10;
    private final ApplicationConfig.Scheduler schedulerConfig;
    private final LinkService linkService;
    private final ChatLinkService chatLinkService;
    private final GitHubService gitHubService;
    private final StackOverflowService stackOverflowService;
    private final BotApiClient botApiClient;

    @Autowired
    public LinkUpdaterScheduler(
            ApplicationConfig applicationConfig,
            LinkService linkService,
            ChatLinkService chatLinkService,
            GitHubService gitHubService,
            StackOverflowService stackOverflowService,
            BotApiClient botApiClient) {
        this.schedulerConfig = applicationConfig.scheduler();
        this.linkService = linkService;
        this.chatLinkService = chatLinkService;
        this.gitHubService = gitHubService;
        this.stackOverflowService = stackOverflowService;
        this.botApiClient = botApiClient;
    }

    @Scheduled(fixedDelayString = "#{@scheduler.interval}")
    public void update() {
        Collection<LinkDTO> outdatedLinks =
                linkService.findLinksToCheck(LocalDateTime.now().minusMinutes(CHECK_LINK_AFTER));

        Flux.fromIterable(outdatedLinks)
                .flatMap(this::checkAndUpdateLink)
                .subscribe(
                        updatedLink -> LOGGER.info("Link updated: {}", updatedLink.getUrl()),
                        error -> LOGGER.error("Error updating link", error),
                        () -> LOGGER.info("Update process completed."));
    }

    private Mono<LinkDTO> checkAndUpdateLink(LinkDTO link) {
        if (link.getUrl().contains("github.com") && link.getUrl().contains("pull")) {
            return checkGitHubLink(link);
        } else if (link.getUrl().contains("stackoverflow.com")) {
            return checkStackOverflowLink(link);
        }
        return Mono.just(updateLinkLastCheckTime(link, LocalDateTime.now()));
    }

    private Mono<LinkDTO> checkGitHubLink(LinkDTO link) {
        String owner = GitHubLinkExtractor.extractOwner(link.getUrl());
        String repo = GitHubLinkExtractor.extractRepo(link.getUrl());
        int pullRequestId = GitHubLinkExtractor.extractPullRequestId(link.getUrl());

        return gitHubService.getPullRequestInfo(owner, repo, pullRequestId).flatMap(combinedInfo -> {
            List<Comment> updatedComments = Stream.concat(
                            combinedInfo.getIssueComments().stream(), combinedInfo.getPullComments().stream())
                    .filter(comment -> link.getLastUpdateTime() == null
                            || comment.getUpdatedAt()
                                    .isAfter(link.getLastUpdateTime()
                                            .atZone(ZoneId.systemDefault())
                                            .toInstant()
                                            .atOffset(ZoneOffset.UTC)))
                    .toList();

            if (!updatedComments.isEmpty() && link.getLastUpdateTime() != null) {
                String updateMessage = updatedComments.stream()
                        .map(Comment::getCommentDescription)
                        .collect(Collectors.joining("\n\n---\n\n"));
                LinkUpdateRequest updateRequest = new LinkUpdateRequest(
                        link.getLinkId(),
                        link.getUrl(),
                        updateMessage,
                        "GITHUB_UPDATE",
                        chatLinkService.findAllChatsForLink(link.getLinkId()).stream()
                                .map(ChatLinkDTO::getChatId)
                                .collect(Collectors.toList()));
                return botApiClient
                        .postUpdate(updateRequest)
                        .then(Mono.just(updateLinkWithNewTimes(
                                link, LocalDateTime.now(), OffsetDateTime.now().toLocalDateTime())));
            }
            return Mono.just(updateLinkLastCheckTime(link, LocalDateTime.now()));
        });
    }

    private Mono<LinkDTO> checkStackOverflowLink(LinkDTO link) {
        String questionId = StackOverflowLinkExtractor.extractQuestionId(link.getUrl());

        return stackOverflowService.getCombinedInfo(questionId).flatMap(combinedInfo -> {
            OffsetDateTime latestUpdate = combinedInfo.getLatestUpdate();
            OffsetDateTime comparisonBaseTime = link.getLastUpdateTime() != null
                    ? link.getLastUpdateTime()
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .atOffset(ZoneOffset.UTC)
                    : OffsetDateTime.MIN;

            if (link.getLastUpdateTime() == null || latestUpdate.isAfter(comparisonBaseTime)) {
                List<String> updateMessages = new ArrayList<>();
                QuestionResponse question = combinedInfo.getQuestion();

                combinedInfo.getAnswers().forEach(answer -> {
                    if (answer.getLastActivityDate().isAfter(comparisonBaseTime)) {
                        updateMessages.add(
                                "Добавлен или изменён ответ: https://stackoverflow.com/a/" + answer.getAnswerId());
                    }
                });

                if (updateMessages.isEmpty() && question.getLastActivityDate().isAfter(comparisonBaseTime)) {
                    updateMessages.add("Изменения в вопросе: " + question.getTitle() + "\nhttps://stackoverflow.com/q/"
                            + question.getQuestionId());
                }

                if (!updateMessages.isEmpty() && link.getLastUpdateTime() != null) {
                    String descriptionUpdate = String.join("\n", updateMessages);
                    LinkUpdateRequest updateRequest = new LinkUpdateRequest(
                            link.getLinkId(),
                            link.getUrl(),
                            descriptionUpdate,
                            "STACKOVERFLOW_UPDATE",
                            chatLinkService.findAllChatsForLink(link.getLinkId()).stream()
                                    .map(ChatLinkDTO::getChatId)
                                    .collect(Collectors.toList()));
                    return botApiClient
                            .postUpdate(updateRequest)
                            .then(Mono.just(
                                    updateLinkWithNewTimes(link, LocalDateTime.now(), latestUpdate.toLocalDateTime())));
                }
            }
            return Mono.just(updateLinkLastCheckTime(link, LocalDateTime.now()));
        });
    }

    private LinkDTO updateLinkWithNewTimes(LinkDTO link, LocalDateTime checkTime, LocalDateTime updateTime) {
        link.setLastCheckTime(checkTime);
        link.setLastUpdateTime(updateTime);
        linkService.update(link);
        return link;
    }

    private LinkDTO updateLinkLastCheckTime(LinkDTO link, LocalDateTime checkTime) {
        link.setLastCheckTime(checkTime);
        linkService.update(link);
        return link;
    }
}
