# Описание Pull Request

Реализована асинхронная коммуникация между `bot` и `scrapper` через Apache Kafka, кэширование запросов `/list` в Redis, батчинг уведомлений с выбором режима (`instant`/`digest`), фильтрация уведомлений по пользователям и тегирование ссылок. Настроены Docker Compose и Testcontainers для Kafka, Redis, PostgreSQL. Обеспечена обработка невалидных сообщений через Dead Letter Queue (DLQ). Уведомления абстрагированы с выбором транспорта (Kafka/HTTP) через конфигурацию. Поддерживаются JDBC и JPA для доступа к данным (`app.database-access-type`). Добавлены тегирование ссылок (`addTagToLink`, `findLinksByTag`) и фильтрация уведомлений (`user:<username>`). Все требования протестированы с использованием Testcontainers, включая переработанные тесты для JDBC/JPA.

---

## Чек-лист выполнения требований

---

### Функциональные требования

|                              Требование                              | Выполнение |                                                                              Комментарий                                                                              |
|----------------------------------------------------------------------|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Приложения `bot` и `scrapper` общаются асинхронно через Apache Kafka | [x]        | Реализовано через `KafkaTemplate` в `TrackCommand`, `UntrackCommand`, `ListCommand`. Обработка в `KafkaLinkCommandConsumer`. Топики: `link-commands`, `link-updates`. |
| Используются Docker Compose и Testcontainers                         | [x]        | Docker Compose включает Kafka, Redis, PostgreSQL (`docker-compose.yml`). Testcontainers в тестах (`@Testcontainers`).                                                 |
| Некорректные сообщения отправляются в DLQ                            | [x]        | В `KafkaNotificationService` ошибки JSON отправляются в `link-updates-dlq` (создаётся в `kafka-setup`).                                                               |
| Имена топиков задаются в конфигурации                                | [x]        | Топики (`link-commands`, `link-updates`, `link-updates-dlq`) в `application.yaml` (`app.kafka.topics`).                                                               |
| `bot` кэширует запросы `/list` в Redis                               | [x]        | В `ListCommand` данные кэшируются (`tracked-links:<chatId>`, TTL 10 минут, `RedisTemplate`, `RedisConfig`).                                                           |

---

### Нефункциональные требования

|                   Требование                   | Выполнение |                                                             Комментарий                                                              |
|------------------------------------------------|------------|--------------------------------------------------------------------------------------------------------------------------------------|
| Выбор транспорта через `app.message-transport` | [x]        | Реализовано через `@ConditionalOnProperty` в `KafkaNotificationService`, `HttpNotificationService`. По умолчанию `Kafka`.            |
| Отправка уведомлений как абстракция            | [x]        | Интерфейс `NotificationService` реализован в `KafkaNotificationService`, `HttpNotificationService`. Переиспользуется `ObjectMapper`. |
| Уведомления в Kafka в формате JSON             | [x]        | Сериализация через `ObjectMapper` в `KafkaNotificationService`, `TrackCommand`, `UntrackCommand`.                                    |
| Инвалидация кэша при изменении ссылок          | [x]        | В `TrackCommand`, `UntrackCommand` вызывается `redisTemplate.delete("tracked-links:<chatId>")`.                                      |

---

### Требования второй части (тегирование и JDBC/JPA)

|          Требование          | Выполнение |                                                                     Комментарий                                                                     |
|------------------------------|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| Поддержка тегирования ссылок | [x]        | Реализовано через `Tag` (`@ManyToMany` с `Link`), `TagRepository`, `LinkRepository`. Методы: `addTagToLink`, `removeTagFromLink`, `findLinksByTag`. |
| Поддержка JDBC и JPA         | [x]        | Реализовано через `Jdbc*Service` и `Jpa*Service`. Выбор через `app.database-access-type`. Миграции через Liquibase (`link_tags`, `Exsellent:07`).   |

---

---

### ✅ Обязательные тесты

```markdown
| Требование                                         | Выполнение | Комментарий                                                                                           |
|----------------------------------------------------|------------|--------------------------------------------------------------------------------------------------------|
| Тесты запускают Redis/Kafka в Testcontainers       | [x]        | Контейнеры для Redis, Kafka, PostgreSQL в тестах (`@Testcontainers`, `@Container`).                   |
| Валидное сообщение обрабатывается                 | [x]        | Проверено в `KafkaLinkCommandConsumerTest` (команды `add`, `remove`), `KafkaMessageConsumerTest` (уведомления). |
| Невалидные сообщения в DLQ                        | [x]        | В `KafkaNotificationServiceTest` проверяется отправка в `link-updates-dlq`.                           |
| Кэш перехватывает чтение и инвалидируется         | [x]        | В `ListCommandTest` (чтение), `TrackCommandTest`, `UntrackCommandTest` (инвалидация).                 |
| JSON мапится в DTO                                | [x]        | В `KafkaMessageConsumerTest` (`LinkUpdateRequest`), `KafkaLinkCommandConsumerTest` (команды).         |
| Тесты для JDBC/JPA                                | [x]        | Переработаны для `JdbcChatLinkService`, `JpaChatLinkService`, `JdbcLinkService`, `JpaLinkService`.
|                                                    |             |  Проверяют добавление/удаление ссылок, чатов, тегов. |
| Тесты для тегирования                             | [x]        | Проверяют `addTagToLink`, `removeTagFromLink`, `findLinksByTag` в `LinkRepositoryTest`, `LinkServiceTest`. |
| Тесты для батчинга                                | [x]        | Проверяют отправку дайджеста, пустой Redis, невалидный JSON (`NotificationBatchServiceTest`).         |
| Тесты для планировщика                            | [x]        | Проверяют обновления GitHub PR, форматирование сообщений (`LinkUpdaterSchedulerTest`).                |
```

---

### 🌟 Бонусное задание

#### Батчинг уведомлений (25 баллов)

```
| Требование                        | Выполнение | Комментарий                                                                                                     |
|-----------------------------------|------------|----------------------------------------------------------------------------------------------------------------|
| Отправка дайджеста по расписанию | [x]        | В `NotificationBatchService` (`@Scheduled(cron = "${app.batch.notification-cron:0 0 10 * * ?}")`). Отправка в 10:00. |
| Конфигурируемое время отправки   | [x]        | Через `app.batch.notification-cron` в `application.yaml`.                                                     |
| Выбор режима: `instant` или `digest` | [x]     | В `SettingsCommand` (`/settings instant|digest`). Хранится в Redis (`notification-mode:<chatId>`).            |
| Накопление состояния в Redis     | [x]        | Уведомления в режиме `digest` сохраняются в `notifications:<chatId>` (`KafkaMessageConsumer`). Очищаются после отправки. |
```

#### Фильтрация уведомлений (15 баллов)

```
| Требование                                  | Выполнение | Комментарий                                                                                                     |
|---------------------------------------------|------------|----------------------------------------------------------------------------------------------------------------|
| Фильтр `user=<username>` игнорирует обновления | [x]     | Фильтры парсятся в `TrackCommand` (`parseFilters`, формат `key:value`). Хранятся в `ChatLink` (JSON, `JdbcChatLinkService`, `JpaChatLinkService`). Применяются в `LinkUpdaterScheduler` (GitHub PR/issues, StackOverflow). |
```

---

### Реализация

- **Асинхронная коммуникация**:
  - `bot`: Отправка команд (`/track`, `/untrack`, `/list`) в Kafka (`KafkaTemplate`, `TrackCommand`, `UntrackCommand`, `ListCommand`).
  - `scrapper`: Обработка в `KafkaLinkCommandConsumer`, уведомления через `KafkaNotificationService`.
  - Топики: `link-commands`, `link-updates`, `link-updates-dlq` (создаются в `kafka-setup`).
- **Кэширование**:
  - В `ListCommand` кэш в Redis (`tracked-links:<chatId>`, TTL 10 минут, `RedisConfig`).
  - Инвалидация в `TrackCommand`, `UntrackCommand` (`redisTemplate.delete`).
- **DLQ**:
  - Ошибки JSON в `KafkaNotificationService` отправляются в `link-updates-dlq`.
- **Батчинг**:
  - Уведомления в режиме `digest` сохраняются в Redis (`notifications:<chatId>`, `KafkaMessageConsumer`).
  - Дайджест отправляется в `NotificationBatchService` (CRON: 10:00, `app.batch.notification-cron`).
  - Режим выбирается через `/settings` (`SettingsCommand`, Redis `notification-mode:<chatId>`).
- **Фильтрация**:
  - В `TrackCommand` парсятся фильтры (`user:a.s.biryukov`, `parseFilters`).
  - Хранятся в `ChatLink` (JSON, `JdbcChatLinkService`, `JpaChatLinkService`).
  - Применяются в `LinkUpdaterScheduler` (фильтр `user` для GitHub PR/issues, StackOverflow).
- **Тегирование**:
  - Сущность `Tag` (`@ManyToMany` с `Link`), таблица `link_tags` (Liquibase).
  - Методы: `addTagToLink`, `removeTagFromLink`, `findLinksByTag` (`JdbcLinkService`, `JpaLinkService`).
  - Ввод тегов в `TrackCommand` (`AWAITING_TAGS`).
- **JDBC/JPA**:
  - Сервисы: `JdbcChatLinkService`, `JdbcLinkService`, `JpaChatLinkService`, `JpaLinkService`.
  - Выбор через `app.database-access-type` (`JDBC` или `JPA`).
  - Миграции: `link_tags`, `changeset Exsellent:07`.
- **Тесты**:
  - Testcontainers для Redis, Kafka, PostgreSQL.
  - Проверены: Kafka, DLQ, кэш, JSON, батчинг, тегирование, JDBC/JPA, планировщик.

---

