package backend.academy.bot.insadebot;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

    @Bean
    public OpenTelemetrySdk openTelemetrySdk() {
        return OpenTelemetrySdk.builder().setTracerProvider(SdkTracerProvider.builder().build()).build();
    }

    @Bean
    public DisposableBean closeOpenTelemetryResources(OpenTelemetrySdk openTelemetrySdk) {
        return () -> {
            CompletableResultCode result = openTelemetrySdk.getSdkTracerProvider().shutdown();
            result.join(10, TimeUnit.SECONDS);
            if (!result.isSuccess()) {
                System.err.println("OpenTelemetry shutdown timeout.");
            }
        };
    }
}
