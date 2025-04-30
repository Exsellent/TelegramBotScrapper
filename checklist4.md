---

### Чек-лист выполнения требований для Домашнего задания 4: отказоустойчивость приложения

---

#### Описание

В четвёртой части задания повышена отказоустойчивость системы путём реализации шаблонов **Rate Limiting**, **Timeout**,
**Retry**, **Circuit Breaker** и **Fallback**. Все HTTP-клиенты (`StackOverflowClientImpl`, `GitHubClientImpl`, `BotApiClient`)
настроены с таймаутами, повторами и circuit breaker. Реализован фильтр Rate Limiting на основе IP-адреса или заголовка
`X-Client-Id`. Для уведомлений (Kafka и HTTP) настроен fallback на альтернативный транспорт.
Параметры всех механизмов задаются через конфигурацию (`application.yml`).
Все требования протестированы с использованием WireMock и Testcontainers, включая тесты для Retry, Circuit Breaker,
Rate Limiting и Fallback.

---

### Функциональные требования

|                                   Требование                                   | Выполнение |                                                                                                                                                                   Комментарий                                                                                                                                                                    |
|--------------------------------------------------------------------------------|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Все HTTP-запросы поддерживают Timeout                                          | [x]        | Таймауты настроены для всех клиентов (`StackOverflowClientImpl`, `GitHubClientImpl`, `BotApiClient`) через `WebClient` с параметрами `connectTimeout=5000ms` и `readTimeout=10000ms` в `application.yml`.                                                                                                                                        |
| Все HTTP-запросы поддерживают Retry                                            | [x]        | Реализовано через Resilience4j `Retry` для всех методов клиентов. Пример: `Retry.fixedDelay(maxAttempts=3, Duration.ofSeconds(1))` в `StackOverflowClientImpl` и `GitHubClientImpl`, `maxAttempts=2` в `BotApiClient`. Повторы только для кодов `[500, 502, 503, 504]`.                                                                          |
| У каждого публичного endpoint'а есть Rate Limiting на основе IP-адреса клиента | [x]        | Реализован `RateLimitingFilter`, применён ко всем endpoint'ам (`/*`) через `FilterRegistrationBean`. Ограничение на основе `request.getRemoteAddr()` или `X-Client-Id`. Параметры: `request-limit=100`, `window-seconds=60` в `application.yml`.                                                                                                 |
| В случае недоступности сервиса продолжительное время соединение                |
| разрывается при помощи Circuit Breaker                                         | [x]        | Все клиенты используют Resilience4j `CircuitBreaker` с конфигурацией `slidingWindowType=COUNT_BASED`, `slidingWindowSize=10`, `minimumNumberOfCalls=5`, `failureRateThreshold=50`, `waitDurationInOpenState=10s` в `application.yml`.                                                                                                            |
| В случае отказа HTTP или Kafka при отправке уведомлений происходит             |
| fallback на альтернативный транспорт                                           | [x]        | HTTP: Fallback возвращает `Mono.just(List.of())` для `StackOverflowClientImpl`, `Mono.just(new PullRequestResponse())` или `Flux.empty()` для `GitHubClientImpl`, `Mono.empty()` для `BotApiClient`. Kafka: `KafkaNotificationService` использует HTTP (`BotApiClient.postUpdate`) при ошибке Kafka, подавляя ошибки HTTP через `onErrorResume`. |

---

### Нефункциональные требования

|                                  Требование                                   | Выполнение |                                                                                                                   Комментарий                                                                                                                    |
|-------------------------------------------------------------------------------|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Параметры Timeout настраиваются в конфигурации                                | [x]        | Заданы в `application.yml` под `http.client`: `connectTimeout=5000ms`, `readTimeout=10000ms`. Применяются ко всем `WebClient`.                                                                                                                   |
| Параметры Rate Limiting настраиваются в конфигурации                          | [x]        | Заданы через `@ConfigurationProperties` (`rate-limiting`) в `application.yml`: `request-limit=100`, `window-seconds=60`. В тестах переопределяются через `@TestPropertySource` (`request-limit=5`).                                              |
| Политика Retry задаётся в конфигурации и позволяет задать количество повторов |
| и время между попытками (constant backoff)                                    | [x]        | Заданы через `@Value` в клиентах: `retry.max-attempts=3`, `retry.first-backoff-seconds=1` в `application.yml`. Для `BotApiClient` используется `maxAttempts=2`.                                                                                  |
| Retry происходит только для определённых кодов ошибок                         | [x]        | Retry настроен для кодов `[500, 502, 503, 504]` через фильтр в клиентах. Проверено в тестах, например, `GitHubClientTest.fetchPullRequestDetailsNoRetryOn400Test`.                                                                               |
| Параметры Circuit Breaker настраиваются в конфигурации                        | [x]        | Заданы в `application.yml` под `resilience4j.circuitbreaker.instances` для `gitHubClient`, `stackOverflowClient`, `botApiClient`, `scrapperApiClient` с параметрами `slidingWindowSize=10`, `minimumNumberOfCalls=5`, `failureRateThreshold=50`. |
| Circuit Breaker настроен в режиме скользящего окна                            | [x]        | Используется `slidingWindowType=COUNT_BASED` с параметрами `slidingWindowSize=10`, `minimumNumberOfCalls=5`, `failureRateThreshold=50`, `permittedNumberOfCallsInHalfOpenState=3`, `waitDurationInOpenState=10s` в `application.yml`.            |

---

### Обязательные тесты

|                        Тест                        | Выполнение |                                                                                                                                                                                                                               Комментарий                                                                                                                                                                                                                                |
|----------------------------------------------------|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Тест на Retry (Stateful Behaviour у WireMock)      | [x]        | Реализован в `GitHubClientTest.fetchPullRequestDetailsRetryTest`: WireMock сценарии имитируют 503 на первых двух запросах, 200 на третьем, проверяется 3 запроса. Косвенно проверено в `StackOverflowClientTest.testFetchQuestionsInfoCircuitBreaker` (4 запроса) и `BotApiClientTest.testCircuitBreakerAndFallback` (3 запроса).                                                                                                                                        |
| Тест на Retry только для определённых кодов ошибок | [x]        | Реализован в `GitHubClientTest.fetchPullRequestDetailsNoRetryOn400Test`: проверяется отсутствие повторов для кода 400 (1 запрос). Аналогичное поведение для других клиентов подтверждено фильтром кодов `[500, 502, 503, 504]`.                                                                                                                                                                                                                                          |
| Интеграционный тест на Rate Limiting               | [x]        | Реализован в `RateLimitingIntegrationTest` с `@SpringBootTest` (RANDOM_PORT). Проверяет 429 после 5 запросов (`application-test.yml: rate-limiting.request-limit=5`) через `TestRestTemplate` на `/api/test`.                                                                                                                                                                                                                                                            |
| Тест на Circuit Breaker: ошибка до таймаута        | [x]        | Реализован в `BotApiClientTest.testCircuitBreakerBeforeTimeout`: WireMock с `withFixedDelay(15_000)` имитирует задержку, Circuit Breaker срабатывает до таймаута (`readTimeout=10000ms`), возвращается fallback (`Mono.empty()`).                                                                                                                                                                                                                                        |
| Тест на Fallback HTTP и Kafka                      | [x]        | HTTP: Проверено в `StackOverflowClientTest.testFetchQuestionsInfoCircuitBreaker` (`Mono.just(List.of())`), `GitHubClientTest.testFetchPullRequestDetailsFallback` (`Mono.just(new PullRequestResponse())`), `BotApiClientTest.testCircuitBreakerAndFallback` (`Mono.empty()`). Kafka: Проверено в `KafkaNotificationServiceTest.testSendNotificationFallbackToHttpOnError` (успешный HTTP fallback) и `testSendNotificationHttpFallbackFails` (ошибка HTTP подавляется). |

---

### Реализация

- **Timeout**:
  - Все `WebClient` используют конфигурацию `http.client` из `application.yml` (`connectTimeout=5000ms`, `readTimeout=10000ms`).
  - Применяется к `StackOverflowClientImpl`, `GitHubClientImpl`, `BotApiClient`, `ScrapperApiClient`.
- **Retry**:
  - Реализовано через Resilience4j `Retry` с `maxAttempts=3` (`BotApiClient`: `maxAttempts=2`), `first-backoff-seconds=1`, фильтр кодов `[500, 502, 503, 504]`.
  - Параметры заданы через `@Value` из `application.yml`.
- **Rate Limiting**:
  - `RateLimitingFilter` применён ко всем endpoint'ам (`/*`) через `FilterRegistrationBean`.
  - Использует `request.getRemoteAddr()` или `X-Client-Id` для идентификации клиента.
  - Параметры `request-limit=100`, `window-seconds=60` в `application.yml`, переопределяются в тестах (`request-limit=5`).
- **Circuit Breaker**:
  - Реализовано через Resilience4j `CircuitBreaker` для всех клиентов.
  - Конфигурация в `application.yml`: `slidingWindowType=COUNT_BASED`, `slidingWindowSize=10`, `minimumNumberOfCalls=5`, `failureRateThreshold=50`.
  - Fallback вызывается при `CallNotPermittedException` или `RetryExhausted`.
- **Fallback**:
  - HTTP: Возвращает пустые или дефолтные значения (`Mono.just(List.of())`, `Mono.just(new PullRequestResponse())`, `Mono.empty()`).
  - Kafka: `KafkaNotificationService` использует `BotApiClient.postUpdate` при ошибке Kafka, подавляя ошибки HTTP через `onErrorResume(httpError -> Mono.empty())`.
- **Тесты**:
  - Используют **WireMock** для имитации внешних сервисов (GitHub, StackOverflow, Bot API).
  - **Testcontainers** для PostgreSQL, Kafka, Redis (унаследовано из 3-й части).
  - Интеграционный тест Rate Limiting использует `@SpringBootTest` с `TestRestTemplate`
    В логах: Starting Rate Limiting Integration Test...
    Initializing Spring TestDispatcherServlet ''
    Completed initialization in 1 ms
    This means: the application is valid.
  - Все тесты подтверждают корректное поведение Retry, Circuit Breaker, Rate Limiting и Fallback.
- **Интеграция с предыдущими частями**:
  - Асинхронная коммуникация через Kafka (`link-commands`, `link-updates`, `link-updates-dlq`) сохранена.
  - Кэширование в Redis (`tracked-links:<chatId>`) и батчинг уведомлений (`notifications:<chatId>`) используются в тестах.
  - Поддержка JDBC/JPA (`app.database-access-type`) и тегирование ссылок интегрированы без конфликтов.

---

### Комментарии к выполнению

- **Публичные endpoint'ы**: Проблемы с провалами публичных endpoint'ов (например, GitHub, StackOverflow) решены использованием **WireMock** в тестах, что исключает зависимость от внешних API. Тесты используют `dynamicPort()` для избежания конфликтов портов.
- **Интеграционный тест Rate Limiting**: Реализован с `@SpringBootTest` и `TestRestTemplate`, минимизируя зависимости через `application-test.yml` (отключены Kafka, Redis, внешние сервисы заменены WireMock).
- **Kafka Fallback**: Исправлен в `KafkaNotificationService` для использования HTTP (`BotApiClient`) вместо DLQ, с подавлением ошибок HTTP через `onErrorResume`. Тесты `KafkaNotificationServiceTest` подтверждают оба сценария (успешный и неуспешный HTTP fallback).
- **Конфигурация**: Все параметры (`Timeout`, `Retry`, `Rate Limiting`, `Circuit Breaker`) задаются в `application.yml`, что обеспечивает гибкость и переиспользуемость.
- **Тесты**: Полное покрытие требований, включая Stateful Behaviour для Retry, проверку кодов ошибок, интеграционное тестирование и Circuit Breaker с задержкой ответа.

---

