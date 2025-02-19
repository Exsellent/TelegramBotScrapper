package backend.academy.bot.backoff;

/** Реализация экспоненциального отката. */
public final class ExponentialBackOff implements BackOffStrategy {
    private final long initialDelay;
    private final double multiplier;
    private long currentDelay;

    /**
     * Создает стратегию с экспоненциальным увеличением задержки.
     *
     * @param initDelay начальная задержка (мс)
     * @param factor коэффициент увеличения
     */
    public ExponentialBackOff(final long initDelay, final double factor) {
        this.initialDelay = initDelay;
        this.multiplier = factor;
        this.currentDelay = initDelay;
    }

    @Override
    public void backOff() throws InterruptedException {
        Thread.sleep(currentDelay);
        currentDelay = Math.round(currentDelay * multiplier);
    }

    @Override
    public void reset() {
        this.currentDelay = initialDelay;
    }
}
