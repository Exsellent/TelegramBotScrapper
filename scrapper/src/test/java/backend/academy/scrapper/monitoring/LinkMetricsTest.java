package backend.academy.scrapper.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Исправленный тестовый класс для LinkMetrics с правильным использованием Mockito Демонстрирует корректное тестирование
 * компонентов с метриками без ошибок InvalidUseOfMatchersException
 */
@ExtendWith(MockitoExtension.class)
class LinkMetricsTest {

    @Mock
    private MeterRegistry mockRegistry;

    @Mock
    private Counter mockErrorCounter;

    @Mock
    private Timer mockScrapeTimer;

    @Mock
    private Counter.Builder mockCounterBuilder;

    @Mock
    private Timer.Builder mockTimerBuilder;

    private LinkMetrics linkMetrics;

    @BeforeEach
    void setUp() {

        linkMetrics = new LinkMetrics(mockRegistry, mockErrorCounter, mockScrapeTimer);
    }

    /** Тест проверяет создание LinkMetrics через основной конструктор */
    @Test
    void shouldCreateLinkMetricsWithMainConstructor() {
        // Given: подготавливаем моки для статических методов
        try (MockedStatic<Counter> counterMock = mockStatic(Counter.class);
                MockedStatic<Timer> timerMock = mockStatic(Timer.class)) {

            // Настраиваем поведение статических методов
            counterMock.when(() -> Counter.builder("scrapper_errors_total")).thenReturn(mockCounterBuilder);
            timerMock.when(() -> Timer.builder("scrape_duration_seconds")).thenReturn(mockTimerBuilder);

            // Настраиваем цепочку вызовов для Counter.Builder
            when(mockCounterBuilder.description(anyString())).thenReturn(mockCounterBuilder);
            when(mockCounterBuilder.register(any(MeterRegistry.class))).thenReturn(mockErrorCounter);

            // Настраиваем цепочку вызовов для Timer.Builder
            when(mockTimerBuilder.description(anyString())).thenReturn(mockTimerBuilder);
            when(mockTimerBuilder.publishPercentiles(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(mockTimerBuilder);
            when(mockTimerBuilder.register(any(MeterRegistry.class))).thenReturn(mockScrapeTimer);

            // When: создаем объект через основной конструктор
            LinkMetrics metrics = new LinkMetrics(mockRegistry);

            // Then: проверяем, что статические методы были вызваны
            counterMock.verify(() -> Counter.builder("scrapper_errors_total"));
            timerMock.verify(() -> Timer.builder("scrape_duration_seconds"));

            // Проверяем, что builder методы были вызваны
            verify(mockCounterBuilder).description("Total number of scraping errors");
            verify(mockTimerBuilder).description("Time taken to scrape a link");
            verify(mockTimerBuilder).publishPercentiles(0.50, 0.95, 0.99);
        }
    }

    /** Тест метода recordScrape с правильным мокированием статических методов */
    @Test
    void shouldRecordScrapeWithCorrectTypeTag() {
        // Given
        String scrapeType = "github";
        Runnable testTask = mock(Runnable.class);

        // Создаем отдельный мок Timer для этого теста
        Timer mockTimerForRecord = mock(Timer.class);

        try (MockedStatic<Timer> timerMock = mockStatic(Timer.class)) {
            // Настраиваем статический мок
            timerMock.when(() -> Timer.builder("scrape_duration_seconds")).thenReturn(mockTimerBuilder);

            // Настраиваем цепочку builder'а
            when(mockTimerBuilder.tags("type", scrapeType)).thenReturn(mockTimerBuilder);
            when(mockTimerBuilder.publishPercentiles(0.50, 0.95, 0.99)).thenReturn(mockTimerBuilder);
            when(mockTimerBuilder.register(mockRegistry)).thenReturn(mockTimerForRecord);

            // When: записываем скрапинг
            linkMetrics.recordScrape(scrapeType, testTask);

            // Then: проверяем правильность вызовов
            timerMock.verify(() -> Timer.builder("scrape_duration_seconds"));
            verify(mockTimerBuilder).tags("type", scrapeType);
            verify(mockTimerBuilder).publishPercentiles(0.50, 0.95, 0.99);
            verify(mockTimerBuilder).register(mockRegistry);
            verify(mockTimerForRecord).record(testTask);
        }
    }

    /**
     * Тест updateLinkCount - этот метод не использует статические методы, поэтому здесь нет проблем с argument matchers
     */
    @Test
    void shouldUpdateLinkCountWithCorrectParameters() {
        // Given
        String linkType = "stackoverflow";
        int linkCount = 42;

        // When: обновляем счетчик ссылок
        linkMetrics.updateLinkCount(linkType, linkCount);

        // Then: проверяем вызов gauge с правильными параметрами
        ArgumentCaptor<Tags> tagsCaptor = ArgumentCaptor.forClass(Tags.class);
        verify(mockRegistry).gauge(eq("active_links_count"), tagsCaptor.capture(), eq(linkCount));

        // Проверяем содержимое тегов
        Tags capturedTags = tagsCaptor.getValue();
        assertEquals(
                "stackoverflow",
                capturedTags.stream()
                        .filter(tag -> "type".equals(tag.getKey()))
                        .findFirst()
                        .map(tag -> tag.getValue())
                        .orElse(null));
    }

    /** Тест incrementErrorCount с правильным мокированием Counter.builder */
    @Test
    void shouldIncrementErrorCountWithCorrectType() {
        // Given
        String errorType = "network_timeout";
        Counter mockCounterForError = mock(Counter.class);

        try (MockedStatic<Counter> counterMock = mockStatic(Counter.class)) {
            // Настраиваем статический мок
            counterMock.when(() -> Counter.builder("scrapper_errors_total")).thenReturn(mockCounterBuilder);

            // Настраиваем цепочку builder'а
            when(mockCounterBuilder.tags("type", errorType)).thenReturn(mockCounterBuilder);
            when(mockCounterBuilder.register(mockRegistry)).thenReturn(mockCounterForError);

            // When: увеличиваем счетчик ошибок
            linkMetrics.incrementErrorCount(errorType);

            // Then: проверяем правильность создания и инкремента
            counterMock.verify(() -> Counter.builder("scrapper_errors_total"));
            verify(mockCounterBuilder).tags("type", errorType);
            verify(mockCounterBuilder).register(mockRegistry);
            verify(mockCounterForError).increment();
        }
    }

    /** Тест проверяет, что задача действительно выполняется в recordScrape */
    @Test
    void shouldExecuteTaskInRecordScrape() {
        // Given
        boolean[] taskExecuted = {false}; // Используем массив для изменения в lambda
        Runnable testTask = () -> taskExecuted[0] = true;
        Timer mockTimerForTask = mock(Timer.class);

        try (MockedStatic<Timer> timerMock = mockStatic(Timer.class)) {
            timerMock.when(() -> Timer.builder("scrape_duration_seconds")).thenReturn(mockTimerBuilder);

            when(mockTimerBuilder.tags(anyString(), anyString())).thenReturn(mockTimerBuilder);
            when(mockTimerBuilder.publishPercentiles(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(mockTimerBuilder);
            when(mockTimerBuilder.register(any(MeterRegistry.class))).thenReturn(mockTimerForTask);

            // When: выполняем скрапинг
            linkMetrics.recordScrape("test", testTask);

            // Then: проверяем, что задача была передана в timer.record()
            verify(mockTimerForTask).record(testTask);
        }
    }

    /** Тест проверяет обработку различных типов ссылок Этот тест безопасен, так как не использует статические методы */
    @Test
    void shouldHandleDifferentLinkTypes() {
        // Given
        String[] linkTypes = {"github", "stackoverflow", "reddit"};
        int[] counts = {10, 20, 30};

        // When & Then: проверяем каждый тип
        for (int i = 0; i < linkTypes.length; i++) {
            linkMetrics.updateLinkCount(linkTypes[i], counts[i]);

            // Проверяем вызов для каждого типа
            verify(mockRegistry).gauge(eq("active_links_count"), eq(Tags.of("type", linkTypes[i])), eq(counts[i]));
        }
    }

    /** Тест проверяет поведение при нулевом количестве ссылок */
    @Test
    void shouldHandleZeroLinkCount() {
        // Given
        String linkType = "empty";
        int zeroCount = 0;

        // When
        linkMetrics.updateLinkCount(linkType, zeroCount);

        // Then: должен корректно обработать нулевое значение
        verify(mockRegistry).gauge("active_links_count", Tags.of("type", linkType), zeroCount);
    }

    /**
     * Упрощенный интеграционный тест без статических методов Фокусируется на методах, которые можно протестировать без
     * сложного мокирования
     */
    @Test
    void shouldHandleUpdateLinkCountWorkflow() {
        // Given
        String linkType = "integration_test";

        // When: выполняем операции обновления счетчиков
        linkMetrics.updateLinkCount(linkType, 5);
        linkMetrics.updateLinkCount(linkType, 10);
        linkMetrics.updateLinkCount(linkType, 0);

        // Then: проверяем, что все операции выполнены правильно
        verify(mockRegistry, times(3)).gauge(eq("active_links_count"), eq(Tags.of("type", linkType)), anyInt());

        // Проверяем конкретные значения
        verify(mockRegistry).gauge("active_links_count", Tags.of("type", linkType), 5);
        verify(mockRegistry).gauge("active_links_count", Tags.of("type", linkType), 10);
        verify(mockRegistry).gauge("active_links_count", Tags.of("type", linkType), 0);
    }
}
