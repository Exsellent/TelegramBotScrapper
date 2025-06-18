package backend.academy.scrapper.database.scheduler;

import backend.academy.scrapper.service.LinkService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MetricsScheduler {
    private final LinkService linkService;

    public MetricsScheduler(LinkService linkService) {
        this.linkService = linkService;
    }

    @Scheduled(fixedRate = 60000) // Каждую минуту
    public void updateMetrics() {
        linkService.registerActiveLinksMetrics();
    }
}
