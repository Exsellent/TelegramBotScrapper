package backend.academy.monitoring;

import static org.mockito.Mockito.*;

import backend.academy.bot.monitoring.BotMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BotMetricsTest {

    private Counter messageCounter;
    private Counter errorCounter;
    private Timer updateProcessingTimer;
    private BotMetrics botMetrics;

    @BeforeEach
    void setUp() {
        messageCounter = mock(Counter.class);
        errorCounter = mock(Counter.class);
        updateProcessingTimer = mock(Timer.class);
        botMetrics = new BotMetrics(messageCounter, errorCounter, updateProcessingTimer);
    }

    @Test
    void incrementMessageCount_ShouldIncrementCounter() {
        botMetrics.incrementMessageCount();
        verify(messageCounter).increment();
    }

    @Test
    void incrementErrorCount_ShouldIncrementCounter() {
        botMetrics.incrementErrorCount("test");
        verify(errorCounter).increment();
    }

    @Test
    void recordUpdateProcessingTime_ShouldRecordTime() {
        Runnable action = mock(Runnable.class);
        botMetrics.recordUpdateProcessingTime(action, true);
        verify(updateProcessingTimer).record(action);
    }

    @Test
    void init_ShouldInitializeMetrics() {
        botMetrics.init();
        verify(messageCounter).increment();
        verify(errorCounter).increment();
        verify(updateProcessingTimer).record(any(Runnable.class));
    }
}
