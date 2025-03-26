package backend.academy.scrapper.jpa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.scrapper.domain.Chat;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.repository.repository.ChatRepository;
import backend.academy.scrapper.repository.repository.LinkRepository;
import backend.academy.scrapper.service.ChatService;
import jakarta.persistence.EntityManager;
import java.io.File;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {"app.database-access-type=jpa"})
@Testcontainers
public class JpaChatServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaChatServiceTest.class);

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("scrapper")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeAll
    static void init() throws Exception {
        LOGGER.info("Starting PostgreSQL container...");
        postgres.start();
        LOGGER.info("PostgreSQL container started with JDBC URL: {}", postgres.getJdbcUrl());

        LOGGER.info("Applying Liquibase migrations...");
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(postgres.createConnection("")));

        File projectRoot = new File("C:/Java/IntelIDEA/java-Exsellent");
        if (!projectRoot.exists()) {
            LOGGER.error("Project root directory does not exist: {}", projectRoot.getAbsolutePath());
            throw new IllegalStateException("Project root not found");
        }

        File changelogFile = new File(projectRoot, "migrations/db/changelog-master.xml");
        LOGGER.info("Changelog file exists: {}", changelogFile.exists());
        if (!changelogFile.exists()) {
            LOGGER.error("Changelog file not found at: {}", changelogFile.getAbsolutePath());
            throw new IllegalStateException("Changelog file not found");
        }

        Liquibase liquibase = new Liquibase(
                "migrations/db/changelog-master.xml", new FileSystemResourceAccessor(projectRoot), database);
        liquibase.update(new Contexts());
        LOGGER.info("Liquibase migrations applied successfully.");
    }

    @BeforeEach
    @Transactional
    void setup() {
        LOGGER.info("Setting up test data...");
        // Очистка всех связанных таблиц
        chatRepository.deleteAll();
        linkRepository.deleteAll();
        // Синхронизация с базой данных
        chatRepository.flush();
        linkRepository.flush();
        // Очистка кэша Hibernate
        entityManager.clear();
        // Проверка, что таблица пуста
        assertEquals(0, chatRepository.count(), "Chat table should be empty after setup");
    }

    @Test
    @Transactional
    void register_CreatesNewChat_WhenChatDoesNotExist() {
        LOGGER.info("Running register_CreatesNewChat_WhenChatDoesNotExist...");
        final long chatId = 1L;
        chatService.register(chatId);
        Chat chat = chatRepository.findById(chatId).orElse(null);
        assertNotNull(chat);
        assertEquals(chatId, chat.getChatId());
        LOGGER.info("Chat registered with ID: {}", chat.getChatId());
    }

    @Test
    void unregister_ThrowsChatNotFoundException_WhenChatDoesNotExist() {
        LOGGER.info("Running unregister_ThrowsChatNotFoundException_WhenChatDoesNotExist...");
        final long chatId = 1L;
        LOGGER.info("Chat exists before unregister: {}", chatRepository.existsById(chatId));
        ChatNotFoundException thrown = assertThrows(ChatNotFoundException.class, () -> chatService.unregister(chatId));
        assertTrue(thrown.getMessage().contains("Chat with ID " + chatId + " not found."));
        LOGGER.info("Exception thrown as expected: {}", thrown.getMessage());
    }
}
