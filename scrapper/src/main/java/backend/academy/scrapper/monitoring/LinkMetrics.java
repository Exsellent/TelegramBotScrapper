package backend.academy.scrapper.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class LinkMetrics {
    private final MeterRegistry registry;

    public LinkMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordScrape(String type, Runnable scrapeTask) {
        Timer timer = Timer.builder("scrape_duration_seconds")
                .tag("type", type)
                .publishPercentiles(0.50, 0.95, 0.99)
                .register(registry);
        timer.record(scrapeTask);
    }

    public void updateLinkCount(String type, int count) {
        registry.gauge("active_links_count", Tags.of("type", type), count);
    }
}
