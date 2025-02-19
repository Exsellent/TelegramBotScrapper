package backend.academy.bot.backoff;

/** Интерфейс стратегии отката. */
public interface BackOffStrategy {

    /**
     * Выполняет задержку перед повторной попыткой.
     *
     * @throws InterruptedException если поток был прерван
     */
    void backOff() throws InterruptedException;

    /** Сбрасывает задержку к начальному значению. */
    void reset();
}
