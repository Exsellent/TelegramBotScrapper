package backend.academy.scrapper.database.scheduler;

import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.dto.Comment;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.dto.LinkUpdateRequest;
import backend.academy.scrapper.dto.QuestionResponse;
import backend.academy.scrapper.service.ChatLinkService;
import backend.academy.scrapper.service.GitHubService;
import backend.academy.scrapper.service.LinkService;
import backend.academy.scrapper.service.NotificationService;
import backend.academy.scrapper.service.StackOverflowService;
import backend.academy.scrapper.utils.GitHubLinkExtractor;
import backend.academy.scrapper.utils.StackOverflowLinkExtractor;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class LinkUpdaterScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkUpdaterScheduler.class);
    private static final int BATCH_SIZE = 1000;
    private static final int PARALLEL_THREADS = 4;
    private static final int PREVIEW_LENGTH = 200;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LinkService linkService;
    private final ChatLinkService chatLinkService;
    private final GitHubService gitHubService;
    private final StackOverflowService stackOverflowService;
    private final NotificationService notificationService;
    private final int checkIntervalMinutes;

    @Autowired
    public LinkUpdaterScheduler(
            LinkService linkService,
            ChatLinkService chatLinkService,
            GitHubService gitHubService,
            StackOverflowService stackOverflowService,
            NotificationService notificationService,
            @Value("${app.check-interval-minutes}") int checkIntervalMinutes) {
        this.linkService = linkService;
        this.chatLinkService = chatLinkService;
        this.gitHubService = gitHubService;
        this.stackOverflowService = stackOverflowService;
        this.notificationService = notificationService;
        this.checkIntervalMinutes = checkIntervalMinutes;
    }

    @Scheduled(fixedDelayString = "#{@scheduler.interval}")
    public void update() {
        int offset = 0;
        Collection<LinkDTO> links;
        do {
            links = linkService.findLinksToCheck(
                    LocalDateTime.now().minusMinutes(checkIntervalMinutes), offset, BATCH_SIZE);
            LOGGER.info("Processing batch of {} links at offset {}", links.size(), offset);
            processLinksBatch(links);
            offset += BATCH_SIZE;
        } while (!links.isEmpty());
    }

    private void processLinksBatch(Collection<LinkDTO> batchLinks) {
        int chunkSize = (batchLinks.size() + PARALLEL_THREADS - 1) / PARALLEL_THREADS;
        List<LinkDTO> linksList = new ArrayList<>(batchLinks);

        Flux.range(0, PARALLEL_THREADS)
                .flatMap(threadIndex -> {
                    int chunkStart = threadIndex * chunkSize;
                    int chunkEnd = Math.min(chunkStart + chunkSize, linksList.size());
                    if (chunkStart >= linksList.size()) {
                        return Flux.empty();
                    }
                    List<LinkDTO> chunk = linksList.subList(chunkStart, chunkEnd);
                    LOGGER.info("Thread {} processing {} links", threadIndex, chunk.size());
                    return Flux.fromIterable(chunk)
                            .flatMap(this::checkAndUpdateLink)
                            .publishOn(Schedulers.boundedElastic());
                })
                .collectList()
                .doOnSuccess(result -> LOGGER.info("Batch processed successfully"))
                .doOnError(error -> LOGGER.error("Error processing batch", error))
                .subscribe();
    }

    private Mono<LinkDTO> checkAndUpdateLink(LinkDTO link) {
        List<ChatLinkDTO> chats = new ArrayList<>(chatLinkService.findAllChatsForLink(link.getLinkId()));
        Map<String, String> filters = chatLinkService.getFiltersForLink(link.getLinkId());

        if (link.getUrl().contains("github.com")) {
            if (link.getUrl().contains("pull")) {
                String owner = GitHubLinkExtractor.extractOwner(link.getUrl());
                String repo = GitHubLinkExtractor.extractRepo(link.getUrl());
                int pullRequestId = GitHubLinkExtractor.extractPullRequestId(link.getUrl());

                return gitHubService
                        .getPullRequestInfo(owner, repo, pullRequestId)
                        .flatMap(combinedInfo -> {
                            Stream<Comment> comments = Stream.concat(
                                    combinedInfo.getIssueComments().stream().map(comment -> (Comment) comment),
                                    combinedInfo.getPullComments().stream().map(comment -> (Comment) comment));
                            return processGitHubComments(
                                    link,
                                    comments,
                                    chats,
                                    filters,
                                    "**PR Update**: ",
                                    "GITHUB_UPDATE",
                                    combinedInfo.getPullRequest().getTitle(),
                                    combinedInfo.getPullRequest().getCreatedAt());
                        });
            } else if (link.getUrl().contains("issues")) {
                String owner = GitHubLinkExtractor.extractOwner(link.getUrl());
                String repo = GitHubLinkExtractor.extractRepo(link.getUrl());
                int issueId = GitHubLinkExtractor.extractIssueId(link.getUrl());

                return gitHubService.getIssueInfo(owner, repo, issueId).flatMap(combinedInfo -> {
                    Stream<Comment> comments =
                            combinedInfo.getIssueComments().stream().map(comment -> (Comment) comment);
                    return processGitHubComments(
                            link,
                            comments,
                            chats,
                            filters,
                            "**Issue Update**: ",
                            "GITHUB_ISSUE_UPDATE",
                            combinedInfo.getPullRequest().getTitle(),
                            combinedInfo.getPullRequest().getCreatedAt());
                });
            }
        } else if (link.getUrl().contains("stackoverflow.com")) {
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
                    QuestionResponse question = combinedInfo.getQuestion();
                    StringBuilder updateMessageBuilder = new StringBuilder();
                    updateMessageBuilder
                            .append("**Question Update**: ")
                            .append(question.getTitle())
                            .append("\n")
                            .append("**User**: ")
                            .append(question.getOwner().getName())
                            .append("\n")
                            .append("**Time**: ")
                            .append(question.getLastActivityDate().format(FORMATTER))
                            .append("\n\n");

                    AtomicBoolean hasChanges = new AtomicBoolean(false);

                    combinedInfo.getAnswers().forEach(answer -> {
                        if (answer.getLastActivityDate().isAfter(comparisonBaseTime)
                                && (!filters.containsKey("user")
                                        || !filters.get("user")
                                                .equals(answer.getOwner().getName()))) {
                            hasChanges.set(true);
                            updateMessageBuilder
                                    .append("---\n")
                                    .append("New/Updated Answer by ")
                                    .append(answer.getOwner().getName())
                                    .append(" at ")
                                    .append(answer.getLastActivityDate().format(FORMATTER))
                                    .append(":\n")
                                    .append(answer.getBody()
                                            .replaceAll("<[^>]*>", "")
                                            .substring(
                                                    0,
                                                    Math.min(
                                                            PREVIEW_LENGTH,
                                                            answer.getBody().length())))
                                    .append("...\n")
                                    .append("https://stackoverflow.com/a/")
                                    .append(answer.getAnswerId())
                                    .append("\n");
                        }
                    });

                    if (hasChanges.get()) {
                        LinkUpdateRequest updateRequest = new LinkUpdateRequest(
                                link.getLinkId(),
                                link.getUrl(),
                                updateMessageBuilder.toString(),
                                "STACKOVERFLOW_UPDATE",
                                chats.stream().map(ChatLinkDTO::getChatId).collect(Collectors.toList()));
                        notificationService.sendNotification(updateRequest);
                        return Mono.just(
                                updateLinkWithTimes(link, LocalDateTime.now(), latestUpdate.toLocalDateTime()));
                    }
                }
                return Mono.just(updateLinkLastCheck(link, LocalDateTime.now()));
            });
        }
        return Mono.just(updateLinkLastCheck(link, LocalDateTime.now()));
    }

    private Mono<LinkDTO> processGitHubComments(
            LinkDTO link,
            Stream<Comment> comments,
            List<ChatLinkDTO> chats,
            Map<String, String> filters,
            String updateType,
            String notificationType,
            String title,
            OffsetDateTime createdAt) {
        List<Comment> updatedComments = comments.filter(comment -> (link.getLastUpdateTime() == null
                                || comment.getUpdatedAt()
                                        .isAfter(link.getLastUpdateTime()
                                                .atZone(ZoneId.systemDefault())
                                                .toInstant()
                                                .atOffset(ZoneOffset.UTC)))
                        && (!filters.containsKey("user")
                                || !filters.get("user").equals(comment.getUser().getName())))
                .collect(Collectors.toList());

        if (!updatedComments.isEmpty() && link.getLastUpdateTime() != null) {
            String updateMessage = buildGitHubUpdateMessage(updateType, title, createdAt, updatedComments);
            LinkUpdateRequest updateRequest = new LinkUpdateRequest(
                    link.getLinkId(),
                    link.getUrl(),
                    updateMessage,
                    notificationType,
                    chats.stream().map(ChatLinkDTO::getChatId).collect(Collectors.toList()));
            notificationService.sendNotification(updateRequest);
            return Mono.just(updateLinkWithTimes(
                    link, LocalDateTime.now(), OffsetDateTime.now().toLocalDateTime()));
        }
        return Mono.just(updateLinkLastCheck(link, LocalDateTime.now()));
    }

    private String buildGitHubUpdateMessage(
            String updateType, String title, OffsetDateTime createdAt, List<Comment> comments) {
        StringBuilder updateMessageBuilder = new StringBuilder();

        updateMessageBuilder.append(updateType).append(title).append("\n");
        updateMessageBuilder
                .append("**Created**: ")
                .append(createdAt.format(FORMATTER))
                .append("\n\n");

        for (Comment comment : comments) {
            updateMessageBuilder.append("---\n");
            updateMessageBuilder
                    .append("Comment by ")
                    .append(comment.getUser().getName())
                    .append(" at ")
                    .append(comment.getUpdatedAt().format(FORMATTER))
                    .append(":\n");

            String commentText = comment.getCommentDescription();
            String preview = commentText.length() > PREVIEW_LENGTH
                    ? commentText.substring(0, PREVIEW_LENGTH) + "..."
                    : commentText;
            updateMessageBuilder.append(preview).append("\n");
        }

        return updateMessageBuilder.toString();
    }

    private LinkDTO updateLinkWithTimes(LinkDTO link, LocalDateTime checkTime, LocalDateTime updateTime) {
        link.setLastCheckTime(checkTime);
        link.setLastUpdateTime(updateTime);
        linkService.update(link);
        return link;
    }

    private LinkDTO updateLinkLastCheck(LinkDTO link, LocalDateTime checkTime) {
        link.setLastCheckTime(checkTime);
        linkService.update(link);
        return link;
    }
}
