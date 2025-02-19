package backend.academy.bot.backoff;

/** Реализация стратегии отката с линейным увеличением времени ожидания. */
public final class LinearBackOff implements BackOffStrategy {
    /** Начальная задержка в миллисекундах. */
    private final long initialDelay;

    /** Значение, на которое увеличивается задержка после каждой попытки. */
    private final long increment;

    /** Текущая задержка в миллисекундах. */
    private long currentDelay;

    /**
     * Создает новый экземпляр линейной стратегии отката.
     *
     * @param initDelay начальная задержка в миллисекундах
     * @param stepIncrement шаг увеличения задержки
     */
    public LinearBackOff(final long initDelay, final long stepIncrement) {
        this.initialDelay = initDelay;
        this.increment = stepIncrement;
        this.currentDelay = initDelay;
    }

    /**
     * Выполняет задержку перед повторной попыткой с линейным увеличением времени ожидания.
     *
     * @throws InterruptedException если поток был прерван во время ожидания
     */
    @Override
    public void backOff() throws InterruptedException {
        Thread.sleep(currentDelay);
        currentDelay += increment;
    }

    /** Сбрасывает задержку к начальному значению. */
    @Override
    public void reset() {
        this.currentDelay = initialDelay;
    }
}
