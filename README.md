![Build](https://github.com/central-university-dev/backend-academy-2025-spring-template/actions/workflows/build.yaml/badge.svg)

---

---

# Link Tracker

Проект создан в рамках курса "Академия Бэкенда".
Telegram-бот для отслеживания обновлений по ссылкам на GitHub и StackOverflow.
Проект написан на `Java 23` с использованием `Spring Boot 3.4.2` и состоит из двух приложений:
- **Bot**: Telegram-бот для взаимодействия с пользователем.
- **Scrapper**: Сервис для обработки ссылок, проверки обновлений и отправки уведомлений.

Для работы требуются PostgreSQL, Kafka, Redis, Prometheus и Grafana.
Миграции базы данных выполняются через Liquibase. Тесты используют Testcontainers.

В третьем модуле реализовано:
- асинхронное взаимодействие между `bot` и `scrapper` через Kafka,
- кэширование запросов `/list` в Redis,
- батчинг уведомлений (дайджест),
- фильтрация по пользователю,
- обработка невалидных сообщений через DLQ,
- конфигурация выбора транспорта (`Kafka`/`HTTP`),
- тегирование ссылок,
- поддержка `JDBC` и `JPA` (выбор через `app.database-access-type`).

В пятом модуле добавлен мониторинг:

- Метрики через Micrometer и Prometheus.
- Дашборды в Grafana (RED-метрики, использование памяти, бизнес-метрики).
- Эндпоинты `/metrics` на портах `8083` (scrapper) и `8084` (bot).
- Тесты метрик с использованием Testcontainers.

---

## 📌 Возможности

### Команды бота

- `/start` — регистрация пользователя.
- `/help` — справка по командам.
- `/track` — добавление ссылки с тегами и фильтрами.
- `/untrack` — удаление ссылки.
- `/list` — просмотр всех отслеживаемых ссылок.
- `/settings` — выбор режима уведомлений (`instant` / `digest`).

### Уведомления об обновлениях

Scrapper отправляет детализированные уведомления через Kafka:
- **Для StackOverflow (ответ или комментарий)**:
- Текст вопроса.
- Имя пользователя.
- Время создания.
- Превью ответа/комментария (до 200 символов).
- **Для GitHub (PR, Issue, комментарий)**:
- Название PR/Issue.
- Имя пользователя.
- Время создания.
- Превью описания/комментария (до 200 символов).

- **Режимы уведомлений**:
  - `instant`: Уведомления отправляются сразу.
  - `digest`: Уведомления накапливаются в Redis и отправляются дайджестом (по умолчанию в 10:00,
    настраивается через `app.batch.notification-cron`).

### Тегирование ссылок

- Пользователи добавляют теги к ссылкам (например, "Работа", "Хобби") через `/track` (опционально,
  можно пропустить с помощью "skip").
- Поддерживаются операции:
  - Добавление тега (`addTagToLink`).
  - Удаление тега (`removeTagFromLink`).
  - Поиск ссылок по тегу (`findLinksByTag`).

### Фильтрация уведомлений

- Пользователи задают фильтры в `/track` (формат `key:value`, например, `user:a.s.biryukov`)
  для игнорирования обновлений.
- Фильтры сохраняются в базе (`ChatLink`) и частично применяются при проверке обновлений.

### Мониторинг (Модуль 5)

- **Метрики**:
  - `bot_messages_total`: Количество сообщений, обработанных ботом (скорость: `rate(bot_messages_total[5m])`).
  - `scrapper.links.active`: Количество активных ссылок в БД по типу (`github`, `stackoverflow`).
  - `scrape_duration_seconds`: Время выполнения scrape для GitHub и StackOverflow (p50, p95, p99).
  - `jvm_memory_used_bytes`: Использование памяти JVM (heap, non-heap).
  - RED-метрики: Rate, Errors, Duration для HTTP-запросов.
- **Инфраструктура**:
  - Prometheus (`:9090`) собирает метрики через `/actuator/prometheus` (`:8083` для scrapper, `:8084` для bot).
  - Grafana (`:3001`) отображает дашборды:
    - Параметризованный дашборд для RED-метрик.
    - График использования памяти.
    - Бизнес-метрики (сообщения бота, активные ссылки, время scrape).
- **Тесты**:
  - `BotMetricsTest`: Проверяет `bot_messages_total`, `bot_errors_total`, `bot_update_processing`.
  - `LinkMetricsTest`: Проверяет `scrapper_errors_total`, `scrapper.links.active`, `scrape_duration_seconds`.

---

## ⚙️ Архитектура

### Компоненты

- `bot` — Telegram-бот, принимает команды, кэширует список ссылок.
- `scrapper` — обрабатывает ссылки, отслеживает обновления, отправляет уведомления, регистрирует метрики.
- Kafka используется для связи между компонентами.
- Redis — кэш и накопление уведомлений.
- Prometheus — сбор метрик.
- Grafana — визуализация дашбордов.

---

## 🧪 Тестирование

Тесты используют Testcontainers:

- PostgreSQL, Kafka, Redis, Prometheus запускаются автоматически.
- Проверяется: Kafka, Redis, DLQ, фильтры, тегирование, дайджест, JDBC, JPA, метрики.

Запуск тестов:

```bash
mvn test
```

---

## 🔧 Установка

1. Установите:
   - Java 23+
   - Maven 3.8.8+
   - Docker
2. Клонируйте репозиторий:

```bash
git clone <repo-url>
cd <project>
```

---

## 📁 Конфигурация

Создайте `.env`:

```env
POSTGRES_DB=scrapper
POSTGRES_USER=postgres
POSTGRES_PASSWORD=secret
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
REDIS_HOST=redis
REDIS_PORT=6379
GITHUB_TOKEN=ghp_XXXX
STACKOVERFLOW_KEY=XXX
STACKOVERFLOW_ACCESS_TOKEN=XXX
```

Пример `application.yaml`:

```yaml
app:
    telegram-token: YOUR_TELEGRAM_TOKEN
    database-access-type: jdbc  # или jpa
    message-transport: Kafka  # или HTTP
    kafka:
        topics:
            link-commands: link-commands
            link-updates: link-updates
            link-updates-dlq: link-updates-dlq
    batch:
        notification-cron: 0 0 10 * * ?  # Дайджест в 10:00

spring:
    kafka:
        bootstrap-servers: kafka:9092
    data:
        redis:
            host: redis
            port: 6379
management:
    server:
        port: 8083  # Для scrapper (8084 для bot)
    endpoints:
        web:
            exposure:
                include: prometheus,health
```

---

## 🚀 Запуск приложения

### 🔧 1. Запуск инфраструктуры

Запустите PostgreSQL, Kafka, Redis, Prometheus и Grafana через Docker:

```bash
docker-compose up -d postgresql kafka redis prometheus grafana
```

Примените миграции Liquibase:

```bash
docker-compose up migrations
```

Проверьте наличие таблиц в базе:

```bash
docker exec -it <postgresql-container-name> psql -U postgres -d scrapper -c "\dt"
```

Ожидаемые таблицы:
- `link`
- `chat`
- `chat_link`
- `tag`
- `link_tags`

> Замените `<postgresql-container-name>` на имя контейнера PostgreSQL.
> Узнать можно через `docker ps`.

---

### ⚙️ 2. Запуск Scrapper

Перейдите в директорию `scrapper`:

```bash
cd scrapper
```

Запустите приложение:

```bash
mvn spring-boot:run
```

Порты:

- Приложение: **8082**
- Метрики: **8083**

---

### 🤖 3. Запуск Bot

Перейдите в директорию `bot`:

```bash
cd ../bot
```

Запустите приложение:

```bash
mvn spring-boot:run
```

Порты:

- Приложение: **8080**
- Метрики: **8084**

---

### 📊 4. Доступ к мониторингу

- **Prometheus**: [http://localhost:9090](http://localhost:9090)
- **Grafana**: [http://localhost:3001](http://localhost:3001)
- **Метрики**:
  - Scrapper: `curl http://localhost:8083/actuator/prometheus`
  - Bot: `curl http://localhost:8084/actuator/prometheus`

---

## 🗂 Структура проекта

```
java-Exsellent/
├── bot/                          # Telegram-бот
│   └── src/
│       ├── main/
│       │   └── java/backend/academy/bot/
│       │       ├── command/           # Команды бота (/track, /untrack, /list, /settings)
│       │       ├── client/            # Клиент для Scrapper API
│       │       ├── configuration/     # Конфигурации Redis, Kafka и приложения
│       │       ├── controller/        # REST-контроллеры
│       │       ├── dto/               # DTO-объекты
│       │       ├── exception/         # Кастомные исключения
│       │       ├── insidebot/         # TelegramBotService и логика бота
│       │       ├── service/           # KafkaService, RedisCacheService, NotificationBatchService и др.
│       │       └── utils/             # Парсеры ссылок
│       └── resources/
│           ├── application.yaml       # Конфигурация
│           └── log4j2-plain.xml       # Логирование

├── scrapper/                     # Микросервис Scrapper
│   └── src/
│       ├── main/
│       │   └── java/backend/academy/scrapper/
│       │       ├── client/             # API-клиенты GitHub и StackOverflow
│       │       ├── configuration/      # Kafka, JPA, Redis и общие конфиги
│       │       ├── controller/         # REST-контроллер Scrapper API
│       │       ├── dao/                # JDBC DAO-интерфейсы
│       │       ├── database/
│       │       │   ├── jdbc/service/   # JDBC-сервисы
│       │       │   ├── jpa/service/    # JPA-сервисы
│       │       │   └── scheduler/      # Планировщик LinkUpdaterScheduler
│       │       ├── domain/             # JPA-сущности (Link, Chat, Tag и т.д.)
│       │       ├── dto/                # DTO-объекты
│       │       ├── filter/             # RateLimit фильтры
│       │       ├── repository/         # Spring Data JPA репозитории
│       │       ├── service/            # Kafka и HTTP Notification сервисы
│       │       └── utils/              # Парсеры ссылок
│       └── resources/
│           ├── application.yaml        # Конфигурация
│           └── log4j2-plain.xml        # Логирование

├── migrations/                 # Liquibase миграции (в корне проекта)
│   ├── db.changelog-master.yaml
│   └── changes/
│       ├── 001_create_tables.sql
│       ├── 007_create_link_tags.sql
│       └── ...

├── docker-compose.yaml         # Инфраструктура: PostgreSQL, Redis, Kafka, Prometheus, Grafana
├── Dockerfile.migrations       # Dockerfile для Liquibase миграций
├── README.md                   # Документация
├── pom.xml                     # Maven конфигурация (мульти-модуль)
└── .env                        # Переменные окружения
```

---

## 📊 Интеграции

- Swagger UI: [http://localhost:8082/swagger-ui](http://localhost:8082/swagger-ui)
- Actuator:
  - Scrapper: `curl http://localhost:8083/actuator/health`
  - Bot: `curl http://localhost:8084/actuator/health`
- Prometheus: [http://localhost:9090](http://localhost:9090)
- Grafana: [http://localhost:3001](http://localhost:3001)

---

## 🛑 Остановка

```bash
docker-compose down
```

---

## 📝 Примечания

- Kafka топики создаются автоматически.
- Redis используется для кэширования (`tracked-links:<chatId>`) и накопления (`notifications:<chatId>`).
- Расписание дайджеста настраивается в `application.yaml`.
- Фильтры хранятся в JSON-поле `filters` у `ChatLink`.
- Линтеры (CPD) настраиваются через `pom.xml` (`minimumTokens=200`).
- CI/CD настроен через GitHub Actions.

---

