package backend.academy.scrapper.jpa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.scrapper.database.dao.jpa.JpaChatDao;
import backend.academy.scrapper.database.dao.jpa.JpaChatLinkDao;
import backend.academy.scrapper.database.dao.jpa.JpaLinkDao;
import backend.academy.scrapper.database.jpa.service.JpaChatLinkService;
import backend.academy.scrapper.domain.Chat;
import backend.academy.scrapper.domain.Link;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {"app.database-access-type=jpa"})
@Testcontainers
public class JpaChatLinkServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaChatLinkServiceTest.class);

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
    @Qualifier("jpaChatLinkService")
    private JpaChatLinkService chatLinkService;

    @Autowired
    private JpaChatDao chatDao;

    @Autowired
    private JpaLinkDao linkDao;

    @Autowired
    private JpaChatLinkDao chatLinkDao;

    private Chat testChat;
    private Link testLink;

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
        // Очистка через DAO
        chatLinkDao
                .findAll()
                .forEach(chatLink -> chatLinkDao.removeByChatIdAndLinkId(chatLink.getChatId(), chatLink.getLinkId()));
        linkDao.findAll().forEach(link -> linkDao.remove(link.getUrl()));
        chatDao.findAll().forEach(chat -> chatDao.remove(chat.getChatId()));

        // Создание тестового чата через DAO
        ChatDTO chatDTO = new ChatDTO(1L, LocalDateTime.now());
        chatDao.add(chatDTO);
        testChat = new Chat();
        testChat.setChatId(chatDTO.getChatId());
        testChat.setCreatedAt(chatDTO.getCreatedAt());

        // Создание тестовой ссылки через DAO
        LinkDTO linkDTO = LinkDTO.builder()
                .url("http://example.com")
                .lastCheckTime(LocalDateTime.now())
                .tags(new java.util.ArrayList<>())
                .build();
        Long linkId = linkDao.add(linkDTO);
        testLink = new Link();
        testLink.setId(linkId);
        testLink.setUrl(linkDTO.getUrl());
        testLink.setLastCheckTime(linkDTO.getLastCheckTime());

        LOGGER.info("Test chat created with ID: {}", testChat.getChatId());
        LOGGER.info("Test link created with ID: {}", testLink.getId());
    }

    @Test
    @Transactional
    void addLinkToChat_ThrowsChatNotFoundException_IfChatDoesNotExist() {
        LOGGER.info("Running addLinkToChat_ThrowsChatNotFoundException_IfChatDoesNotExist...");
        final long nonExistingChatId = -1L;
        ChatNotFoundException thrown = assertThrows(
                ChatNotFoundException.class, () -> chatLinkService.addLinkToChat(nonExistingChatId, testLink.getId()));
        assertTrue(thrown.getMessage().contains("Chat with ID " + nonExistingChatId + " not found."));
        LOGGER.info("Exception thrown as expected: {}", thrown.getMessage());
    }

    @Test
    @Transactional
    void addLinkToChat_AddsLink_IfChatExists() {
        LOGGER.info("Running addLinkToChat_AddsLink_IfChatExists...");
        chatLinkService.addLinkToChat(testChat.getChatId(), testLink.getId());
        Collection<ChatLinkDTO> links = chatLinkService.findAllLinksForChat(testChat.getChatId());
        assertEquals(1, links.size());
        assertTrue(links.stream().anyMatch(link -> link.getLinkId().equals(testLink.getId())));
        LOGGER.info("Link added to chat, found {} links", links.size());
    }

    @Test
    @Transactional
    void findAllLinksForChat_ReturnsCorrectLinks() {
        LOGGER.info("Running findAllLinksForChat_ReturnsCorrectLinks...");
        chatLinkService.addLinkToChat(testChat.getChatId(), testLink.getId());
        Collection<ChatLinkDTO> links = chatLinkService.findAllLinksForChat(testChat.getChatId());
        assertFalse(links.isEmpty());
        assertEquals(1, links.size());
        LOGGER.info("Found {} links for chat", links.size());
    }
}
