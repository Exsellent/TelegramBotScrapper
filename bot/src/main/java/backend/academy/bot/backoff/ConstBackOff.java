package backend.academy.bot.backoff;

/** Реализация стратегии отката с фиксированной задержкой. */
public final class ConstBackOff implements BackOffStrategy {
    /** Время задержки в миллисекундах. */
    private final long backOffPeriod;

    /**
     * Создает стратегию с фиксированной задержкой.
     *
     * @param delay время задержки в миллисекундах
     */
    public ConstBackOff(final long delay) {
        this.backOffPeriod = delay;
    }

    @Override
    public void backOff() throws InterruptedException {
        Thread.sleep(backOffPeriod);
    }

    @Override
    public void reset() {}
}
