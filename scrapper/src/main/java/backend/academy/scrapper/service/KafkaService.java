package backend.academy.scrapper.service;

import backend.academy.scrapper.dto.LinkUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class KafkaService {
    private final KafkaTemplate<String, LinkUpdateRequest> kafkaTemplate;

    @Autowired
    public KafkaService(KafkaTemplate<String, LinkUpdateRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<Void> sendNotification(String topic, LinkUpdateRequest update) {
        return Mono.fromFuture(kafkaTemplate.send(topic, update)).then();
    }
}
