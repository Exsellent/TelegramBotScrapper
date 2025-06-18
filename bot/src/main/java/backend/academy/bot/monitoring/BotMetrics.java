package backend.academy.bot.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BotMetrics {

    private final Counter messageCounter;
    private final Counter errorCounter;
    private final Timer updateProcessingTimer;

    @Autowired
    public BotMetrics(MeterRegistry registry) {
        this(
                Counter.builder("bot_messages_total")
                        .description("Total number of messages processed")
                        .register(registry),
                Counter.builder("bot_errors_total")
                        .description("Total number of errors")
                        .register(registry),
                Timer.builder("bot_update_processing")
                        .description("Time taken to process updates")
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .register(registry));
    }

    // Конструктор для тестов
    public BotMetrics(Counter messageCounter, Counter errorCounter, Timer updateProcessingTimer) {
        this.messageCounter = messageCounter;
        this.errorCounter = errorCounter;
        this.updateProcessingTimer = updateProcessingTimer;
    }

    public void incrementMessageCount() {
        messageCounter.increment();
    }

    public void incrementErrorCount(String errorType) {
        errorCounter.increment();
    }

    public void recordUpdateProcessingTime(Runnable action, boolean success) {
        updateProcessingTimer.record(action);
    }

    @PostConstruct
    public void init() {
        incrementMessageCount();
        incrementErrorCount("test");
        recordUpdateProcessingTime(
                () -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                },
                true);
    }
}
