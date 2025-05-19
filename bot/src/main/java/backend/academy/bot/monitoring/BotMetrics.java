package backend.academy.bot.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class BotMetrics {
    private final MeterRegistry meterRegistry;
    private final Timer updateTimer;

    public BotMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.updateTimer = Timer.builder("bot.update.processing")
                .tag("status", "success")
                .publishPercentiles(0.50, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void recordUpdateProcessingTime(Runnable task, boolean success) {
        Timer timer = Timer.builder("bot.update.processing")
                .tag("status", success ? "success" : "error")
                .publishPercentiles(0.50, 0.95, 0.99)
                .register(meterRegistry);
        timer.record(task);
    }

    public void incrementErrorCount(String errorType) {
        meterRegistry.counter("bot.errors.count", "type", errorType).increment();
    }

    public void incrementMessageCount() {
        meterRegistry.counter("bot_messages_total").increment();
    }
}
