package backend.academy.bot.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationProperties(prefix = "http.client")
@EnableConfigurationProperties(HttpClientProperties.class)
public class HttpClientProperties {
    private int connectTimeout = 5000; // 5 seconds
    private int readTimeout = 10000; // 10 seconds;

    // Getters and setters
    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}
