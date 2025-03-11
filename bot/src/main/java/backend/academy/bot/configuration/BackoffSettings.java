package backend.academy.bot.configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackoffSettings {
    private String strategy = "exponential";
    private long initialDelay = 1000;
    private double multiplier = 2.0;
    private long increment = 1000;
    private int maxAttempts = 5;
}
