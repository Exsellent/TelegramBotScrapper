package backend.academy.scrapper.jdbc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.scrapper.ScrapperApplication;
import backend.academy.scrapper.configuration.JdbcAccessConfiguration;
import backend.academy.scrapper.dao.ChatDao;
import backend.academy.scrapper.dao.ChatLinkDao;
import backend.academy.scrapper.dao.LinkDao;
import backend.academy.scrapper.database.jdbc.service.JdbcChatLinkService;
import backend.academy.scrapper.domain.ChatLink;
import backend.academy.scrapper.dto.ChatDTO;
import backend.academy.scrapper.dto.ChatLinkDTO;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.exception.ChatNotFoundException;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Collection;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@SpringBootTest(
        classes = ScrapperApplication.class,
        properties = {"app.database-access-type=jdbc", "spring.liquibase.enabled=false"})
@ActiveProfiles("test")
@ContextConfiguration(classes = JdbcAccessConfiguration.class)
public class JdbcChatLinkServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcChatLinkServiceTest.class);

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("scrapper")
            .withUsername("java-exsellent-postgresql-1")
            .withPassword("21Kaharaiona");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcChatLinkService chatLinkService;

    @Autowired
    private ChatDao chatDao;

    @Autowired
    private LinkDao linkDao;

    @Autowired
    private ChatLinkDao chatLinkDao;

    private final long testChatId = 1L;
    private long testLinkId;

    @BeforeAll
    static void init() throws Exception {
        LOGGER.info("Starting PostgreSQL container...");
        postgres.start();
        LOGGER.info("PostgreSQL container started with JDBC URL: {}", postgres.getJdbcUrl());

        LOGGER.info("Applying Liquibase migrations...");
        try {
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(postgres.createConnection("")));

            // Поднимаемся на уровень корня проекта из модуля scrapper
            File scrapperDir = new File(System.getProperty("user.dir"));
            File projectRoot = scrapperDir.getParentFile() != null ? scrapperDir.getParentFile() : scrapperDir;
            File changelogFile = new File(projectRoot, "migrations/db/changelog-master.xml");

            LOGGER.info("Looking for changelog file at: {}", changelogFile.getAbsolutePath());
            if (!changelogFile.exists()) {
                LOGGER.error("Changelog file not found at: {}", changelogFile.getAbsolutePath());
                throw new IllegalStateException("Changelog file not found");
            }

            Liquibase liquibase = new Liquibase(
                "migrations/db/changelog-master.xml", new FileSystemResourceAccessor(projectRoot), database);
            liquibase.update(new Contexts());
            LOGGER.info("Liquibase migrations applied successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to apply Liquibase migrations", e);
            throw e;
        }
    }

    @BeforeEach
    void setup() {
        LOGGER.info("Setting up test data...");
        chatDao.add(new ChatDTO(testChatId, LocalDateTime.now()));
        LOGGER.info("Chat added with ID: {}", testChatId);

        LinkDTO link = LinkDTO.builder()
                .url("http://example.com")
                //            .description("Example link description")
                .lastCheckTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        testLinkId = linkDao.add(link);
        LOGGER.info("Link added with ID: {}", testLinkId);

        ChatLink chatLink = new ChatLink();
        chatLink.setChatId(testChatId);
        chatLink.setLinkId(testLinkId);
        chatLink.setSharedAt(LocalDateTime.now());
        chatLinkDao.add(chatLink);
        LOGGER.info("ChatLink added for chatId: {} and linkId: {}", testChatId, testLinkId);
    }

    @Test
    @Transactional
    void shouldAddLinkToChatWhenChatExists() {
        LOGGER.info("Running shouldAddLinkToChatWhenChatExists...");
        LinkDTO secondLink = LinkDTO.builder()
                .url("http://example2.com")
                .description("Second example link description")
                .createdAt(LocalDateTime.now())
                .build();
        Long secondLinkId = linkDao.add(secondLink);
        LOGGER.info("Second link added with ID: {}", secondLinkId);

        chatLinkService.addLinkToChat(testChatId, secondLinkId);
        Collection<ChatLinkDTO> links = chatLinkService.findAllLinksForChat(testChatId);
        LOGGER.info("Found links for chatId {}: {}", testChatId, links);
        assertEquals(2, links.size(), "Expected 2 links for the chat");
        assertTrue(
                links.stream().anyMatch(link -> link.getLinkId().equals(secondLinkId)),
                "Second link should be associated with the chat");
    }

    @Test
    @Transactional
    void shouldThrowExceptionWhenAddingLinkToNonExistentChat() {
        LOGGER.info("Running shouldThrowExceptionWhenAddingLinkToNonExistentChat...");
        Exception exception =
                assertThrows(ChatNotFoundException.class, () -> chatLinkService.addLinkToChat(999L, testLinkId));
        LOGGER.info("Exception thrown: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("Chat with ID 999 not found."));
    }

    @Test
    @Transactional
    void shouldRemoveLinkFromChat() {
        LOGGER.info("Running shouldRemoveLinkFromChat...");
        chatLinkService.removeLinkFromChat(testChatId, testLinkId);
        Collection<ChatLinkDTO> links = chatLinkService.findAllLinksForChat(testChatId);
        LOGGER.info("Links after removal for chatId {}: {}", testChatId, links);
        assertTrue(links.isEmpty(), "Expected no links after removal");
    }

    @Test
    @Transactional
    void shouldFindAllLinksForChat() {
        LOGGER.info("Running shouldFindAllLinksForChat...");
        Collection<ChatLinkDTO> links = chatLinkService.findAllLinksForChat(testChatId);
        LOGGER.info("Found links for chatId {}: {}", testChatId, links);
        assertEquals(1, links.size(), "Expected 1 link for the chat");
        assertTrue(
                links.stream().anyMatch(link -> link.getLinkId().equals(testLinkId)), "Expected link should be found");
    }
}
