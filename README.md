![Build](https://github.com/central-university-dev/backend-academy-2025-spring-template/actions/workflows/build.yaml/badge.svg)

---

# Link Tracker

Проект создан в рамках курса "Академия Бэкенда".
Telegram-бот для отслеживания обновлений по ссылкам на GitHub и StackOverflow.
Проект написан на `Java 23` с использованием `Spring Boot 3.4.2` и состоит на данный момент из двух приложений:
- **Bot**: Telegram-бот для взаимодействия с пользователем.
- **Scrapper**: Сервис для обработки ссылок, проверки обновлений и отправки уведомлений.

Для работы требуются PostgreSQL, Kafka и Redis. Миграции базы данных выполняются через Liquibase.
Тесты используют Testcontainers.

В третьем модуле реализовано:
- асинхронное взаимодействие между `bot` и `scrapper` через Kafka,
- кэширование запросов `/list` в Redis,
- батчинг уведомлений (дайджест),
- фильтрация по пользователю,
- обработка невалидных сообщений через DLQ,
- конфигурация выбора транспорта (`Kafka`/`HTTP`),
- тегирование ссылок,
- поддержка `JDBC` и `JPA` (выбор через `app.database-access-type`).

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
  Фильтры сохраняются в базе (`ChatLink`) и частично применяются при проверке обновлений

---

## ⚙️ Архитектура

### Компоненты

- `bot` — Telegram-бот, принимает команды, кеширует список ссылок.
- `scrapper` — обрабатывает ссылки, отслеживает обновления, отправляет уведомления.
- Kafka используется для связи между компонентами.
- Redis — кэш и накопление уведомлений.

---

## 🧪 Тестирование

Тесты используют Testcontainers:
- PostgreSQL, Kafka, Redis запускаются автоматически.
- Проверяется: Kafka, Redis, DLQ, фильтры, тегирование, дайджест, JDBC и JPA.

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
```

---

## 🚀 Запуск приложения

### 🔧 1. Запуск инфраструктуры

Запустите PostgreSQL, Kafka и Redis через Docker:

```bash
docker-compose up -d postgresql kafka redis
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

Порт: **8082**

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

Порт: **8080**

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
│       │       ├── service/           # KafkaService, RedisCacheService![img.png](img.png), NotificationBatchService и др.
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

├── docker-compose.yaml         # Инфраструктура: PostgreSQL, Redis, Kafka
├── Dockerfile.migrations       # Dockerfile для Liquibase миграций
├── README.md                   # Документация
├── pom.xml                     # Maven конфигурация (мульти-модуль)
└── .env                        # Переменные окружения
```

---

## 📊 Интеграции

- Swagger UI: [http://localhost:8082/swagger-ui](http://localhost:8082/swagger-ui)
- Actuator: `curl http://localhost:8082/api/actuator/health`

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

---


