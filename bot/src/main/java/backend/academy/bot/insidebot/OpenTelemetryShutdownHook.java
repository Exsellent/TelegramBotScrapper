package backend.academy.bot.insidebot;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpenTelemetryShutdownHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryShutdownHook.class);
    private final OpenTelemetrySdk openTelemetrySdk;

    public OpenTelemetryShutdownHook(OpenTelemetrySdk openTelemetrySdk) {
        this.openTelemetrySdk = openTelemetrySdk;
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("Shutting down OpenTelemetry...");

        SdkTracerProvider tracerProvider = openTelemetrySdk.getSdkTracerProvider();
        CompletableResultCode shutdownResult = tracerProvider.shutdown();

        shutdownResult.join(10, TimeUnit.SECONDS);
        if (shutdownResult.isSuccess()) {
            LOGGER.info("OpenTelemetry successfully shut down.");
        } else {
            LOGGER.warn("OpenTelemetry shutdown failed or timed out.");
        }
    }
}
