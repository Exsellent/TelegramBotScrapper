package backend.academy.scrapper.service;

import backend.academy.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.scrapper.dto.AnswerResponse;
import backend.academy.scrapper.dto.CombinedStackOverflowInfo;
import backend.academy.scrapper.dto.QuestionResponse;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class StackOverflowService {
    private final StackOverflowClient stackOverflowClient;
    private final ChatService chatService;

    @Autowired
    public StackOverflowService(StackOverflowClient stackOverflowClient, ChatService chatService) {
        this.stackOverflowClient = stackOverflowClient;
        this.chatService = chatService;
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
        return Mono.zip(getQuestionInfo(questionId), getAnswersForQuestion(questionId), CombinedStackOverflowInfo::new);
    }
}
