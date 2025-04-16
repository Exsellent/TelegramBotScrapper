package backend.academy.scrapper.jdbc.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.database.jdbc.service.JdbcChatService;
import backend.academy.scrapper.dto.ChatDTO;
import backend.academy.scrapper.exception.ChatAlreadyRegisteredException;
import backend.academy.scrapper.exception.ChatNotFoundException;
import java.io.File;
import java.time.LocalDateTime;
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

@SpringBootTest(properties = {"app.database-access-type=jdbc"})
@Testcontainers
public class JdbcChatServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcChatServiceTest.class);

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
    private JdbcChatService chatService;

    @Autowired
    private ChatDao chatDao;

    private final long testChatId = 1L;

    @BeforeAll
    static void init() throws Exception {
        LOGGER.info("Starting PostgreSQL container...");
        postgres.start();
        LOGGER.info("PostgreSQL container started with JDBC URL: {}", postgres.getJdbcUrl());

        LOGGER.info("Applying Liquibase migrations...");
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(postgres.createConnection("")));

        File scrapperDir = new File(System.getProperty("user.dir"));
        File projectRoot = scrapperDir.getParentFile() != null ? scrapperDir.getParentFile() : scrapperDir;
        File changelogFile = new File(projectRoot, "migrations/db/changelog-master.xml");
        if (!projectRoot.exists()) {
            LOGGER.error("Project root directory does not exist: {}", projectRoot.getAbsolutePath());
            throw new IllegalStateException("Project root not found");
        }

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
        if (chatDao.existsById(testChatId)) {
            chatDao.remove(testChatId);
            LOGGER.info("Chat with ID {} removed", testChatId);
        }
    }

    @Test
    @Transactional
    void register_CreatesNewChat_WhenChatDoesNotExist() {
        LOGGER.info("Running register_CreatesNewChat_WhenChatDoesNotExist...");
        chatService.register(testChatId);
        assertTrue(chatDao.existsById(testChatId), "Chat should be created");
        LOGGER.info("Chat with ID {} created successfully", testChatId);
    }

    @Test
    @Transactional
    void register_ThrowsChatAlreadyRegisteredException_WhenChatExists() {
        LOGGER.info("Running register_ThrowsChatAlreadyRegisteredException_WhenChatExists...");
        chatDao.add(createChatDTO(testChatId));
        Exception exception =
                assertThrows(ChatAlreadyRegisteredException.class, () -> chatService.register(testChatId));
        assertTrue(exception.getMessage().contains("Chat with id " + testChatId + " already exists."));
        LOGGER.info("Exception thrown as expected: {}", exception.getMessage());
    }

    @Test
    @Transactional
    void unregister_RemovesChat_WhenChatExists() {
        LOGGER.info("Running unregister_RemovesChat_WhenChatExists...");
        chatDao.add(createChatDTO(testChatId));
        chatService.unregister(testChatId);
        assertFalse(chatDao.existsById(testChatId), "Chat should be removed");
        LOGGER.info("Chat with ID {} removed successfully", testChatId);
    }

    @Test
    @Transactional
    void unregister_ThrowsChatNotFoundException_WhenChatDoesNotExist() {
        LOGGER.info("Running unregister_ThrowsChatNotFoundException_WhenChatDoesNotExist...");
        Exception exception = assertThrows(ChatNotFoundException.class, () -> chatService.unregister(testChatId));
        assertTrue(exception.getMessage().contains("Chat with Id " + testChatId + " not found."));
        LOGGER.info("Exception thrown as expected: {}", exception.getMessage());
    }

    private ChatDTO createChatDTO(long chatId) {
        return new ChatDTO(chatId, LocalDateTime.now());
    }
}
