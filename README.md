![Build](https://github.com/central-university-dev/backend-academy-2025-spring-template/actions/workflows/build.yaml/badge.svg)

# Link Tracker


Проект сделан в рамках курса Академия Бэкенда.

Приложение для отслеживания обновлений контента по ссылкам.
При появлении новых событий отправляется уведомление в Telegram.

Проект написан на `Java 23` с использованием `Spring Boot 3`.

Проект состоит из 2-х приложений:
* Bot
* Scrapper

Для работы требуется БД `PostgreSQL`. Присутствует опциональная зависимость на `Kafka`.

Этот проект представляет собой Telegram-бот, который позволяет пользователям отслеживать ссылки на
GitHub и StackOverflow. Бот предоставляет базовые функции для регистрации пользователей, управления
отслеживаемыми ссылками и уведомления о простых обновлениях. Он взаимодействует с сервисом скреппера
через HTTP по заданному OpenAPI-контракту и включает планировщик для проверки изменений по ссылкам.

## Описание

В рамках данного проекта реализован минимально жизнеспособный продукт (MVP) Telegram-бота. 
Основное внимание уделено сетевой части, логике обработки команд и базовому планировщику задач.
Бот поддерживает диалоговый режим для команды `/track` с использованием машины состояний, а также
базовую обработку ошибок и уведомлений.

## Функциональные возможности

- **Команды бота:**
    - `/start` — регистрация пользователя в системе.
    - `/help` — вывод списка доступных команд с описанием.
    - `/track` — начало отслеживания ссылки (реализовано в формате диалога).
    - `/untrack` — прекращение отслеживания указанной ссылки.
    - `/list` — отображение списка отслеживаемых ссылок; если список пуст, выводится специальное
  сообщение.
- **Обработка неизвестных команд:** Бот уведомляет пользователя, если команда не распознана.
- **Планировщик:** Периодически проверяет обновления по отслеживаемым ссылкам и отправляет простое
уведомление (заглушку)
  при обнаружении изменений.
- **Сетевые вызовы:** Взаимодействие с сервисом скреппера через HTTP в соответствии с OpenAPI-контрактом.

## Установка

1. Убедитесь, что у вас установлены:
    - Java 23 или выше.
    - Maven 3.8.8 или выше.
2. Склонируйте репозиторий:
   ```shell
   git clone <repository-url>
   ```
3. Перейдите в директорию проекта:
   ```shell
   cd <project-directory>
   ```
4. Соберите проект:
   ```shell
   mvn clean install
   ```
5. Запустите бота:
   ```shell
   mvn spring-boot:run
   ```

## Конфигурация

Бот требует настройки через файл конфигурации `application.yml`, который должен находиться в директории `src/main/resources`.
Пример содержимого:

```yaml
app:
  telegram-token: YOUR_TELEGRAM_TOKEN

scrapper:
  api:
    base-url: http://localhost:8080/api
```

- Замените `YOUR_TELEGRAM_TOKEN` на токен вашего Telegram-бота, полученный от BotFather.
- Убедитесь, что `base-url` соответствует адресу сервиса скреппера.

Дополнительные параметры, такие как уровень логирования или настройки планировщика, также можно указать в этом файле.

## Использование

1. Запустите приложение, как указано в разделе "Установка".
2. Найдите вашего бота в Telegram и начните взаимодействие с помощью команд:

    - **`/start`** — Регистрация пользователя.  
      Ответ бота: "Welcome!" (или уведомление, если чат уже зарегистрирован).
    - **`/help`** — Вывод списка команд.  
      Пример ответа:
      ```
      Available Commands:
      /start: Register
      /track: Start tracking the link
      /untrack: Stop tracking the link
      /list: Show a list of tracked links
      
      Supported link formats:
      GitHub: https://github.com/example/repo
      StackOverflow: https://stackoverflow.com/questions/example
      ```
    - **`/track`** — Начало отслеживания ссылки в диалоговом формате:  
      Пример:
      ```
      > /track
      < Please enter the URL you want to track:
      > https://github.com/example/repo
      < Enter tags for this link (optional, space-separated) or type 'skip':
      > work hobby
      < Enter filters (format: key:value, e.g., 'user:john type:comment') or type 'skip':
      > user:john type:comment
      < Link successfully added with specified tags and filters!
      ```
    - **`/untrack <URL>`** — Прекращение отслеживания ссылки.  
      Пример:
      ```
      > /untrack https://github.com/example/repo
      < Link removed from tracking: https://github.com/example/repo
      ```
    - **`/list`** — Показать список отслеживаемых ссылок.  
      Пример:
      ```
      Tracked links:
      https://github.com/example/repo
      ```
      Или, если список пуст:
      ```
      The list of tracked links is empty.
      ```

## Диалоговый режим

Команда `/track` реализована с использованием машины состояний для пошагового ввода данных:
1. **Состояние AWAITING_URL**: Ожидание ввода URL.
2. **Состояние AWAITING_TAGS**: Ожидание ввода тегов (опционально, можно пропустить с помощью "skip").
3. **Состояние AWAITING_FILTERS**: Ожидание ввода фильтров (опционально, можно пропустить с помощью "skip").

После успешного завершения диалога данные сохраняются, и пользователь получает подтверждение.

## Регистрация команд в меню

При запуске бот автоматически регистрирует команды через метод Telegram API `setMyCommands`.
Это делает команды видимыми в меню бота в Telegram. Регистрация выполняется классом `BotMenuInitializer`.

## Планировщик

Планировщик (`UpdateScheduler`) запускается каждые 60 секунд (настраивается через аннотацию `@Scheduled`) и проверяет
обновления по отслеживаемым ссылкам через вызов API скреппера. При обнаружении изменений отправляется простое уведомление,
например:
```
Detected changes in https://github.com/example/repo:
Update detected
```

## Тестирование

Проект включает юнит-тесты для проверки ключевых компонентов. Для их запуска выполните:
```shell
mvn test
```

Тесты охватывают:
- Корректность парсинга ссылок.
- Сохранение данных (ссылки, теги, фильтры) в репозитории.
- Обработку неизвестных команд.
- Добавление и удаление ссылок (happy path).
- Обработку дубликатов ссылок.
- Корректность работы планировщика.
- Обработку ошибок HTTP-запросов.

В тестах используются заглушки (mocks) для внешних API (GitHub, StackOverflow), чтобы избежать реальных вызовов.

## Логирование

Бот использует структурное логирование через Log4j2 (конфигурация в `log4j2-plain.xml`). Логи включают ключ-значения
для удобства анализа. Уровень логирования можно настроить в `application.yml`, например:
```yaml
logging:
  level:
    backend.academy.bot: DEBUG
```

## API Контракт

Бот взаимодействует с сервисом скреппера согласно OpenAPI-контракту, доступному по адресу:  
[https://gist.github.com/sanyarnd/e35dc3d4e0c8000205ec5029dac38f5a]
(https://gist.github.com/sanyarnd/e35dc3d4e0c8000205ec5029dac38f5a)

## Структура проекта

```
├── src/
│   ├── main/
│   │   ├── java/backend/academy/bot/
│   │   │   ├── backoff/            # Стратегии повторов (backoff)
│   │   │   ├── app/                # Точка входа (BotApplication)
│   │   │   ├── command/            # Обработчики команд и машина состояний
│   │   │   ├── client/             # Клиент для взаимодействия со скреппером
│   │   │   ├── config/             # Конфигурация приложения
│   │   │   ├── insidebot/          # Логика Telegram-бота
│   │   │   ├── monitoring/         # Метрики
│   │   │   ├── service/            # Сервисная логика
│   │   │   └── utils/              # Утилиты (LinkParser)
│   │   └── resources/
│   │       ├── application.yml     # Конфигурация
│   │       └── log4j2-plain.xml    # Настройки логирования
│   └── test/                       # Тесты
```

## Дополнительные заметки

- **Типобезопасность конфигурации:** Используется `application.yml` для безопасного хранения настроек.
- **HTTP-клиенты:** Реализованы вручную для GitHub и StackOverflow без использования SDK.
- **Повторы (Retry):** Используется механизм повторов с фиксированной задержкой для обработки ошибок API.


---

## Scrapper Service для Link Tracker Bot

### Описание сервиса

Сервис скреппера является частью системы отслеживания ссылок, работающей в связке с Telegram-ботом. Он отвечает
за взаимодействие с внешними API (GitHub и StackOverflow), управление данными о чатах и ссылках, а также за периодическую
проверку обновлений и отправку уведомлений через Bot API. Сервис реализован с использованием Spring Boot и предоставляет
REST API для управления данными, что позволяет боту эффективно взаимодействовать с ним.

Основные задачи сервиса:
- Получение данных о вопросах и ответах с StackOverflow.
- Получение данных о pull request'ах и комментариях с GitHub.
- Хранение информации о чатах и отслеживаемых ссылках.
- Периодическая проверка обновлений по ссылкам и уведомление пользователей через Telegram-бот.

---

## Структура проекта

Проект организован в модульной структуре для обеспечения читаемости и поддерживаемости кода.

```
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── backend/
│   │   │       └── academy/
│   │   │           └── scrapper/
│   │   │               ├── client/                # Клиенты для взаимодействия с внешними API
│   │   │               │   ├── BotApiClient.java  # Клиент для отправки обновлений в Bot API
│   │   │               │   ├── github/            # Клиенты для GitHub API
│   │   │               │   │   ├── GitHubClient.java
│   │   │               │   │   └── GitHubClientImpl.java
│   │   │               │   └── stackoverflow/     # Клиенты для StackOverflow API
│   │   │               │       ├── StackOverflowClient.java
│   │   │               │       └── StackOverflowClientImpl.java
│   │   │               ├── configuration/         # Конфигурационные классы
│   │   │               │   ├── AccessType.java
│   │   │               │   ├── ApplicationConfig.java
│   │   │               │   ├── ClientConfig.java
│   │   │               │   ├── JdbcAccessConfiguration.java
│   │   │               │   ├── JpaAccessConfiguration.java
│   │   │               │   ├── RateLimitingProperties.java
│   │   │               │   ├── RetryConfig.java
│   │   │               │   └── SchedulerConfig.java
│   │   │               ├── controller/            # REST API контроллеры
│   │   │               │   └── ScrapperApiController.java
│   │   │               ├── dao/                   # Интерфейсы доступа к данным
│   │   │               │   ├── ChatDao.java
│   │   │               │   ├── ChatLinkDao.java
│   │   │               │   └── LinkDao.java
│   │   │               ├── database/              # Реализации хранения данных
│   │   │               │   └── jdbc/
│   │   │               │       ├── dao/
│   │   │               │       │   ├── InMemoryChatDao.java
│   │   │               │       │   ├── InMemoryChatLinkDao.java
│   │   │               │       │   ├── InMemoryLinkDao.java
│   │   │               │       │   ├── JdbcChatDao.java
│   │   │               │       │   ├── JdbcChatLinkDao.java
│   │   │               │       │   └── JdbcLinkDao.java
│   │   │               │       └── service/
│   │   │               │           ├── JdbcChatLinkService.java
│   │   │               │           ├── JdbcChatService.java
│   │   │               │           └── JdbcLinkService.java
│   │   │               ├── domain/                # Доменные модели
│   │   │               │   ├── Chat.java
│   │   │               │   ├── ChatLink.java
│   │   │               │   ├── ChatLinkId.java
│   │   │               │   └── Link.java
│   │   │               ├── dto/                   # Объекты передачи данных (DTO)
│   │   │               │   ├── AddLinkRequest.java
│   │   │               │   ├── AnswerResponse.java
│   │   │               │   ├── AnswersApiResponse.java
│   │   │               │   ├── CombinedPullRequestInfo.java
│   │   │               │   ├── CombinedStackOverflowInfo.java
│   │   │               │   ├── LinkUpdateRequest.java
│   │   │               │   ├── PullRequestResponse.java
│   │   │               │   ├── QuestionResponse.java
│   │   │               │   └── QuestionsApiResponse.java
│   │   │               ├── exception/             # Обработка исключений
│   │   │               │   ├── ChatAlreadyRegisteredException.java
│   │   │               │   ├── ChatNotFoundException.java
│   │   │               │   ├── GlobalExceptionHandler.java
│   │   │               │   ├── LinkAlreadyAddedException.java
│   │   │               │   └── LinkNotFoundException.java
│   │   │               ├── filter/                # Фильтры запросов
│   │   │               │   └── RateLimitingFilter.java
│   │   │               ├── repository/            # Репозитории для работы с данными
│   │   │               │   ├── ChatLinkRepository.java
│   │   │               │   ├── ChatRepository.java
│   │   │               │   ├── InMemoryChatRepository.java
│   │   │               │   ├── InMemoryChatRepositoryImpl.java
│   │   │               │   ├── InMemoryLinkRepositoryImpl.java
│   │   │               │   └── LinkRepository.java
│   │   │               ├── scheduler/             # Планировщик обновлений
│   │   │               │   └── LinkUpdaterScheduler.java
│   │   │               ├── service/               # Бизнес-логика
│   │   │               │   ├── ChatLinkService.java
│   │   │               │   ├── ChatService.java
│   │   │               │   ├── GitHubService.java
│   │   │               │   ├── LinkService.java
│   │   │               │   └── StackOverflowService.java
│   │   │               └── utils/                 # Утилиты
│   │   │                   ├── GitHubLinkExtractor.java
│   │   │                   └── StackOverflowLinkExtractor.java
│   │   └── resources/
│   │       ├── application.yaml                   # Основной файл конфигурации
│   │       └── log4j2-plain.xml                  # Настройки логирования
```

---

## Ключевые компоненты

### Клиенты API
- **`BotApiClient`**: Отправляет уведомления об обновлениях ссылок через Bot API с использованием `WebClient`. Использует реактивный подход для асинхронной обработки запросов.
- **`GitHubClientImpl`**: Реализует взаимодействие с GitHub API. Получает данные о pull request'ах, комментариях к issues и pull request'ам. Поддерживает механизм повторов (Retry) для обработки временных сбоев.
- **`StackOverflowClientImpl`**: Реализует взаимодействие с StackOverflow API. Получает информацию о вопросах и ответах по заданным идентификаторам. Также поддерживает Retry.

### Сервисы
- **`GitHubService`**: Обрабатывает запросы к GitHub API через `GitHubClient`. Предоставляет методы для регистрации/удаления чатов и получения информации о pull request'ах.
- **`StackOverflowService`**: Обрабатывает запросы к StackOverflow API через `StackOverflowClient`. Поддерживает получение данных о вопросах и ответах, а также комбинированной информации.
- **`LinkService`**, **`ChatService`**, **`ChatLinkService`**: Управляют данными о ссылках, чатах и их связях. Поддерживают различные механизмы хранения (in-memory, JDBC, JPA).

### Планировщик
- **`LinkUpdaterScheduler`**: Периодически проверяет отслеживаемые ссылки на обновления. Использует заданный интервал (`scheduler.interval`) из конфигурации. При обнаружении изменений отправляет уведомления через `BotApiClient`.

### Фильтр ограничения скорости
- **`RateLimitingFilter`**: Ограничивает количество запросов с одного IP-адреса, предотвращая перегрузку сервиса. При превышении лимита возвращает ошибку `429 Too Many Requests`.

### Хранение данных
- **`InMemoryChatRepositoryImpl`**, **`InMemoryLinkRepositoryImpl`**: Реализации репозиториев для хранения данных в памяти с использованием `ConcurrentHashMap`.
- Поддержка других механизмов хранения (JDBC, JPA) реализована через соответствующие DAO и сервисы в пакете `database`.

### Утилиты
- **`GitHubLinkExtractor`**, **`StackOverflowLinkExtractor`**: Извлекают идентификаторы и метаданные из ссылок GitHub и StackOverflow для упрощения обработки.

---

## Принципы работы

1. **Получение данных**: Клиенты `GitHubClientImpl` и `StackOverflowClientImpl` используют `WebClient` для асинхронных запросов к внешним API. Ответы преобразуются в DTO (например, `PullRequestResponse`, `QuestionResponse`).
2. **Обработка обновлений**: `LinkUpdaterScheduler` запускается по расписанию, проверяет устаревшие ссылки и вызывает соответствующие сервисы для получения актуальной информации. При обнаружении изменений формируется уведомление и отправляется через `BotApiClient`.
3. **Хранение**: Данные о чатах и ссылках хранятся в зависимости от конфигурации (`IN_MEMORY`, `JDBC`, `JPA`), что обеспечивает гибкость в развертывании.
4. **Ограничение нагрузки**: `RateLimitingFilter` защищает сервис от чрезмерного количества запросов.

---

## Зависимости

- **Spring Boot**: Основа проекта, предоставляет REST API, планировщик и конфигурацию.
- **WebFlux/WebClient**: Для реактивного взаимодействия с внешними API.
- **Log4j2**: Для логирования событий.
- **Jackson**: Для сериализации/десериализации JSON.

---

## Инструкция по запуску приложения

```markdown
### Запуск приложения

Для запуска приложения выполните следующие шаги из корневой директории проекта, где находятся
поддиректории `scrapper` и `bot`. Убедитесь, что у вас установлены Java 23 и Maven 3.8.8 или выше.

1. **Остановка предыдущих процессов:**
   - Если у вас уже запущены Java-процессы (например, от предыдущих запусков приложения),
     остановите их, чтобы освободить порты:
     - **Для Windows:** Откройте командную строку и выполните:
       ```shell
       taskkill /F /IM java.exe
       ```
     - **Для Linux/macOS:** Откройте терминал и выполните:
       ```shell
       pkill java
       ```
   - Это важно, чтобы избежать конфликтов из-за занятых портов.

2. **Запуск сервиса скреппера:**
   - Откройте терминал (или командную строку) и перейдите в директорию скреппера:
     ```shell
     cd scrapper
     mvn spring-boot:run
     ```
   - Дождитесь полной инициализации скреппера. Вы увидите сообщение в терминале, 
     что приложение успешно запустилось (например, "Started ScrapperApplication").

3. **Запуск бота:**
   - Откройте новый терминал (не закрывая первый с запущенным скреппером).
   - Из корневой директории проекта перейдите в директорию бота:
     ```shell
     cd bot
     mvn spring-boot:run
     ```
   - Бот запустится и будет готов к взаимодействию через Telegram.

**Примечание:** 
- Убедитесь, что вы начинаете выполнение команд из корневой директории проекта, содержащей папки
  `scrapper` и `bot`. 
- Запускайте бота только после того, как скреппер полностью инициализируется, чтобы избежать проблем
  с подключением.


```
