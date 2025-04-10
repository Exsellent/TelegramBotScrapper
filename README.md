![Build](https://github.com/central-university-dev/backend-academy-2025-spring-template/actions/workflows/build.yaml/badge.svg)

---

# Link Tracker

Проект разработан в рамках курса "Академия Бэкенда". Это приложение для отслеживания обновлений контента по ссылкам с
уведомлениями в Telegram. Во второй части задания добавлена полноценная работа с базой данных PostgreSQL, реализованы
два провайдера доступа к данным (SQL и ORM), а также поддержка тегирования ссылок для группировки подписок.

Проект написан на `Java 23` с использованием `Spring Boot 3` и состоит из тредх приложений:
- **Bot**: Telegram-бот для взаимодействия с пользователем.
- **Scrapper**: Сервис для обработки ссылок и отправки уведомлений.
- **Report**: Приложение для отправки уведомлений в Telegram.

Для работы требуется PostgreSQL, миграции базы данных выполняются через Liquibase. Опционально поддерживается Kafka (будет
реализована в следующем задании).

---

## Описание

Link Tracker — это Telegram-бот, который позволяет пользователям отслеживать обновления по ссылкам на GitHub и StackOverflow,
получать уведомления о новых событиях (ответах, комментариях, pull requests, issues) и группировать ссылки с помощью тегов.
Scrapper-сервис периодически проверяет изменения и отправляет детализированные уведомления через Bot API.

Реализована схема базы данных, добавлены два способа работы с данными (JDBC и JPA), а также
тегирование ссылок для категоризации (например, "Работа" или "Хобби").
Приложение теперь работает "по-настоящему", с использованием PostgreSQL вместо хранения в памяти.

---

## Функциональные возможности

### Команды бота

- `/start` — Регистрация пользователя.
- `/help` — Вывод списка команд с описанием.
- `/track` — Добавление ссылки для отслеживания (диалоговый режим с поддержкой тегов).
- `/untrack` — Удаление ссылки из отслеживания.
- `/list` — Отображение списка отслеживаемых ссылок с тегами.

### Тегирование ссылок

- Пользователи могут добавлять теги к ссылкам (например, "Работа", "Хобби") для группировки подписок.
- Теги задаются в диалоге `/track` после ввода URL (опционально, можно пропустить с помощью "skip").
- Поддерживаются операции:
  - Добавление тега к ссылке.
  - Удаление тега из ссылки.
  - Поиск ссылок по тегу (доступно через сервисный слой).

### Уведомления об обновлениях

Scrapper отправляет детализированные сообщения при обнаружении изменений:
- **Для StackOverflow (новый ответ или комментарий)**:
- Текст темы вопроса.
- Имя пользователя.
- Время создания.
- Превью ответа или комментария (первые 200 символов).
- **Для GitHub (новый PR или Issue)**:
- Название PR или Issue.
- Имя пользователя.
- Время создания.
- Превью описания (первые 200 символов).

---

## Нефункциональные требования

- Данные из базы не загружаются в память целиком — используется пагинация (пакеты по 50–500 записей,
  настраивается в конфигурации).
- Логика проверки ссылок (планировщик) и отправки уведомлений разделены между сервисами.
- Интерфейс `LinkService` имеет две реализации: `JdbcLinkService` (SQL) и `JpaLinkService` (ORM). Выбор
  способа работы задаётся через свойство `app.database-access-type` (`jdbc` или `jpa`).
- База данных PostgreSQL запускается через Docker Compose для разработки и Testcontainers для тестов.
- Миграции базы данных реализованы через Liquibase, схема находится в корне проекта в директории `/migrations/`.
- Тесты проверяют вставку, удаление, обновление данных и корректность уведомлений.

---

## Установка

1. Убедитесь, что установлены:
   - Java 23 или выше.
   - Maven 3.8.8 или выше.
   - Docker (для запуска PostgreSQL и миграций).
2. Склонируйте репозиторий:

   ```shell
   git clone <repository-url>
   ```
3. Перейдите в директорию проекта:

   ```shell
   cd <project-directory>
   ```

---

## Конфигурация

### Настройка окружения

Создайте файл `.env` в корне проекта со следующими переменными:

```
POSTGRES_DB=scrapper
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password
BOT_API_BASEURL=http://localhost:8080
GITHUB_TOKEN=your_github_token
STACKOVERFLOW_KEY=your_stackoverflow_key
STACKOVERFLOW_ACCESS_TOKEN=your_stackoverflow_access_token
```

### Файл конфигурации

Настройки задаются в `scrapper/src/main/resources/application.yaml`:

```yaml
app:
  telegram-token: YOUR_TELEGRAM_TOKEN
  database-access-type: jdbc  # или jpa
  check-interval-minutes: 60

scrapper:
  api:
    base-url: http://localhost:8080/api
```

- `telegram-token`: Токен Telegram-бота от BotFather.
- `database-access-type`: Выбор провайдера (`jdbc` для SQL, `jpa` для ORM).
- `check-interval-minutes`: Интервал проверки обновлений.

---

## Запуск приложения

### Запуск PostgreSQL и миграций

1. Соберите образ Liquibase для миграций:

   ```shell
   docker build -f Dockerfile.migrations -t custom-liquibase:4.29 .
   ```
2. Запустите PostgreSQL через Docker Compose:

   ```shell
   docker-compose up -d postgresql
   ```
3. Примените миграции:

   ```shell
   docker-compose up migrations
   ```
4. Проверьте создание таблиц:

   ```shell
   docker exec -it <postgresql-container-name> psql -U postgres -d scrapper -c "\dt"
   ```

   Ожидаемый вывод: таблицы `link`, `
5. `, `link_tags`, `chat`, `chat_link`.

### Запуск Scrapper

1. Перейдите в директорию `scrapper`:

   ```shell
   cd scrapper
   ```
2. Запустите сервис:

   ```shell
   mvn spring-boot:run
   ```

   Дождитесь сообщения "Started ScrapperApplication".

### Запуск бота

1. Перейдите в директорию `bot`:

   ```shell
   cd ../bot
   ```
2. Запустите бота:

   ```shell
   mvn spring-boot:run
   ```

---

## Использование

1. Найдите бота в Telegram и начните с команды `/start`.
2. Пример работы с тегами:

   ```
   > /track
   < Please enter the URL you want to track:
   > https://github.com/example/repo
   < Enter tags for this link (optional, space-separated) or type 'skip':
   > work hobby
   < Link successfully added with tags: work, hobby
   ```
3. Уведомления об обновлениях приходят автоматически, например:

   ```
   Detected changes in https://github.com/example/repo:
   **PR Update**: Fix bug #123
   **Created**: 2025-03-25 12:00:00
   Comment by john_doe at 2025-03-25 12:05:00:
   Looks good, approved!
   ```

---

## Тестирование

Тесты используют Testcontainers для запуска PostgreSQL. Запустите их командой:

```shell
mvn test
```

Проверяется:
- Вставка, удаление, обновление данных в базе.
- Переключение между `jdbc` и `jpa` через конфигурацию.
- Формат уведомлений для StackOverflow и GitHub (превью 200 символов).

---

## Структура проекта

### Scrapper

```
scrapper/
├── migrations/                    # Схема БД и миграции Liquibase
│   ├── db.changelog-master.yaml
│   └── changes/
│       ├── 001_create_tables.sql
│       └── ...
├── src/
│   ├── main/
│   │   ├── java/backend/academy/scrapper/
│   │   │   ├── client/            # Клиенты API (GitHub, StackOverflow, Bot)
│   │   │   ├── configuration/     # Конфигурации (JDBC, JPA, Scheduler)
│   │   │   ├── controller/        # REST API (ScrapperApiController)
│   │   │   ├── dao/              # Интерфейсы доступа к данным
│   │   │   ├── database/         # Реализации хранения
│   │   │   │   ├── jdbc/service/ # JDBC-сервисы (JdbcLinkService и др.)
│   │   │   │   ├── jpa/service/  # JPA-сервисы (JpaLinkService и др.)
│   │   │   │   └── scheduler/    # Планировщик (LinkUpdaterScheduler)
│   │   │   ├── domain/           # Сущности (Link, Tag, Chat)
│   │   │   ├── dto/             # DTO для передачи данных
│   │   │   ├── repository/       # JPA-репозитории (TagRepository и др.)
│   │   │   ├── service/          # Интерфейсы сервисов
│   │   │   └── utils/            # Утилиты (LinkExtractor)
│   │   └── resources/
│   │       ├── application.yaml  # Конфигурация
│   └── test/                     # Тесты с Testcontainers
```

### Bot

```
bot/
├── src/
│   ├── main/
│   │   ├── java/backend/academy/bot/
│   │   │   ├── command/          # Обработчики команд и машина состояний
│   │   │   ├── client/           # Клиент для Scrapper API
│   │   │   ├── service/          # Логика бота
│   │   │   └── utils/            # Утилиты (LinkParser)
│   │   └── resources/
│   │       ├── application.yml   # Конфигурация
│   └── test/                     # Тесты
```

---

## Дополнительные заметки

- **База данных**: Данные хранятся в PostgreSQL с таблицами `link`, `tags`, `link_tags`, `chat`, `chat_link`.
  Индексы настроены на поля в `WHERE`-условиях.
- **Миграции**: Liquibase применяет SQL-скрипты из `migrations/`.
- **Пагинация**: Данные загружаются пакетами (настраивается в `application.yaml`).
- **Интерфейсы**: `LinkService`, `ChatService` и др. не содержат специфичных для JDBC/JPA типов.

---

## Отладка

- Swagger UI: `http://localhost:8080/swagger-ui`
- Actuator: `curl http://localhost:8080/api/actuator/health`

---

## Остановка

Остановите контейнеры:

```shell
docker-compose down
```

---
