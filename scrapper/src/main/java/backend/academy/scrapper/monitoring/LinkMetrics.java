package backend.academy.scrapper.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class LinkMetrics {
    private final MeterRegistry registry;

    private final Counter errorCounter;
    private final Timer scrapeTimer;

    public LinkMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.errorCounter = Counter.builder("scrapper_errors_total")
                .description("Total number of scraping errors")
                .register(registry);
        this.scrapeTimer = Timer.builder("scrape_duration_seconds")
                .description("Time taken to scrape a link")
                .publishPercentiles(0.50, 0.95, 0.99)
                .register(registry);
    }

    public void recordScrape(String type, Runnable scrapeTask) {
        Timer timer = Timer.builder("scrape_duration_seconds")
                .tags("type", type)
                .publishPercentiles(0.50, 0.95, 0.99)
                .register(registry);
        timer.record(scrapeTask);
    }

    public void updateLinkCount(String type, int count) {
        registry.gauge("active_links_count", Tags.of("type", type), count);
    }

    public void incrementErrorCount(String type) {
        Counter.builder("scrapper_errors_total")
                .tags("type", type)
                .register(registry)
                .increment();
    }

    @PostConstruct
    public void init() {

        updateLinkCount("github", 3);
        incrementErrorCount("test");
        recordScrape("test", () -> {
            try {
                Thread.sleep(15);
            } catch (InterruptedException ignored) {
            }
        });
    }
}
