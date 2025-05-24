package backend.academy.scrapper.service;

import backend.academy.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.scrapper.dto.AnswerResponse;
import backend.academy.scrapper.dto.CombinedStackOverflowInfo;
import backend.academy.scrapper.dto.QuestionResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class StackOverflowService {
    private final StackOverflowClient stackOverflowClient;
    private final ChatService chatService;
    private final Timer scrapeTimer;

    @Autowired
    public StackOverflowService(
            StackOverflowClient stackOverflowClient, ChatService chatService, MeterRegistry meterRegistry) {
        this.stackOverflowClient = stackOverflowClient;
        this.chatService = chatService;
        this.scrapeTimer = Timer.builder("scrape_duration_seconds")
                .tag("type", "stackoverflow")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void registerChat(long chatId) {
        chatService.register(chatId);
    }

    public void unregisterChat(long chatId) {
        chatService.unregister(chatId);
    }

    public Mono<QuestionResponse> getQuestionInfo(String questionId) {
        return stackOverflowClient
                .fetchQuestionsInfo(Collections.singletonList(questionId))
                .flatMap(list -> list.isEmpty() ? Mono.empty() : Mono.just(list.get(0)));
    }

    public Mono<List<AnswerResponse>> getAnswersForQuestion(String questionId) {
        return stackOverflowClient
                .fetchAnswersInfo(Collections.singletonList(questionId))
                .flatMapMany(Flux::fromIterable)
                .collectList();
    }

    public Mono<List<QuestionResponse>> getAllQuestionsInfo(List<String> questionIds) {
        return stackOverflowClient.fetchQuestionsInfo(questionIds);
    }

    public Mono<List<AnswerResponse>> getAllAnswersInfo(List<String> questionIds) {
        return stackOverflowClient.fetchAnswersInfo(questionIds);
    }

    public Mono<CombinedStackOverflowInfo> getCombinedInfo(String questionId) {
        Mono<QuestionResponse> questionMono = getQuestionInfo(questionId);
        Mono<List<AnswerResponse>> answersMono = getAnswersForQuestion(questionId);

        return Mono.zip(questionMono, answersMono).map(tuple -> {
            QuestionResponse question = tuple.getT1();
            List<AnswerResponse> answers = tuple.getT2();
            OffsetDateTime latestUpdate = answers.stream()
                    .map(AnswerResponse::getLastActivityDate)
                    .max(OffsetDateTime::compareTo)
                    .orElse(question.getLastActivityDate());
            return new CombinedStackOverflowInfo(question, answers, latestUpdate);
        });
    }

    public StackOverflowClient getStackOverflowClient() {
        return stackOverflowClient;
    }

    public void fetchUpdates(String url) {
        scrapeTimer.record(() -> {
            // Парсим URL, например: https://stackoverflow.com/questions/123456/title
            Pattern pattern = Pattern.compile("https://stackoverflow\\.com/questions/(\\d+)/.*");
            Matcher matcher = pattern.matcher(url);
            if (matcher.matches()) {
                String questionId = matcher.group(1);
                getCombinedInfo(questionId).block(); // Блокируем для синхронного вызова
            }
        });
    }
}
