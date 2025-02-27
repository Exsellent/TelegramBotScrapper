package backend.academy.bot.configuration;

import lombok.Setter;

@Setter
public class BackoffSettings {
    private String strategy = "exponential";
    private long initialDelay = 1000;
    private double multiplier = 2.0;
    private long increment = 1000;
    private int maxAttempts = 5;

    public String getStrategy() {
        return strategy;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public long getIncrement() {
        return increment;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}
