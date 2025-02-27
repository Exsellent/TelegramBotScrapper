package backend.academy.bot.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class BotMetrics {
    private final MeterRegistry meterRegistry;

    public BotMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordUpdateProcessingTime(Timer.Sample sample, boolean success) {
        sample.stop(meterRegistry.timer("bot.update.processing", "status", success ? "success" : "error"));
    }

    public void incrementRequestCount(String requestType) {
        meterRegistry.counter("bot.requests.count", "type", requestType).increment();
    }

    public void incrementErrorCount(String errorType) {
        meterRegistry.counter("bot.errors.count", "type", errorType).increment();
    }
}
