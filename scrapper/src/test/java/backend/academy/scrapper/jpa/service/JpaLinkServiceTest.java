package backend.academy.scrapper.jpa.service;

import backend.academy.scrapper.database.dao.jpa.JpaChatDao;
import backend.academy.scrapper.database.dao.jpa.JpaLinkDao;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.exception.LinkAlreadyAddedException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.service.LinkService;
import jakarta.persistence.EntityManager;
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

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"app.database-access-type=jpa"})
@Testcontainers
public class JpaLinkServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaLinkServiceTest.class);

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
    private LinkService linkService;

    @Autowired
    private JpaLinkDao linkDao;

    @Autowired
    private JpaChatDao chatDao;

    @Autowired
    private EntityManager entityManager;

    private final String testUrl = "http://example.com/test";
    private final String testDescription = "Test Description";
    private Long testLinkId;

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
        linkDao.findAll().forEach(link -> linkDao.remove(link.getUrl()));
        chatDao.findAll().forEach(chat -> chatDao.remove(chat.getChatId()));

        LinkDTO link = linkService.add(testUrl, testDescription);
        testLinkId = link.getLinkId();
        entityManager.flush();
        LOGGER.info("Test link added with ID: {}", testLinkId);
    }

    @Test
    @Transactional
    void add_ThrowsLinkAlreadyAddedException_WhenLinkExists() {
        LOGGER.info("Running add_ThrowsLinkAlreadyAddedException_WhenLinkExists...");
        entityManager.flush(); // смотрим, что изменения видны
        Exception exception =
                assertThrows(LinkAlreadyAddedException.class, () -> linkService.add(testUrl, "Another Description"));
        assertTrue(exception.getMessage().contains(testUrl + " already exists."));
        LOGGER.info("Exception thrown as expected: {}", exception.getMessage());
    }

    @Test
    @Transactional
    void remove_ThrowsLinkNotFoundException_WhenLinkNotExists() {
        LOGGER.info("Running remove_ThrowsLinkNotFoundException_WhenLinkNotExists...");
        String nonExistentUrl = "http://nonexistent.com";
        Exception exception = assertThrows(LinkNotFoundException.class, () -> linkService.remove(nonExistentUrl));
        assertTrue(exception.getMessage().contains(nonExistentUrl + " not found."));
        LOGGER.info("Exception thrown as expected: {}", exception.getMessage());
    }

    @Test
    @Transactional
    void update_UpdatesLinkSuccessfully() {
        LOGGER.info("Running update_UpdatesLinkSuccessfully...");
        String updatedDescription = "Updated Description";
        LinkDTO updatedLink = LinkDTO.builder()
                .linkId(testLinkId)
                .url(testUrl)
                .description(updatedDescription)
                .createdAt(LocalDateTime.now())
                .lastCheckTime(LocalDateTime.now())
                .lastUpdateTime(LocalDateTime.now())
                .build();
        linkService.update(updatedLink);

        LinkDTO foundLink = linkService.findById(testLinkId);
        assertEquals(updatedDescription, foundLink.getDescription());
        LOGGER.info("Link updated successfully, new description: {}", foundLink.getDescription());
    }

    @Test
    @Transactional
    void findById_ReturnsCorrectLink() {
        LOGGER.info("Running findById_ReturnsCorrectLink...");
        LinkDTO foundLink = linkService.findById(testLinkId);
        assertNotNull(foundLink);
        assertEquals(testUrl, foundLink.getUrl());
        assertEquals(testDescription, foundLink.getDescription());
        LOGGER.info("Link found: {}", foundLink);
    }

    @Test
    @Transactional
    void findByUrl_ReturnsCorrectLink() {
        LOGGER.info("Running findByUrl_ReturnsCorrectLink...");
        LinkDTO foundLink = linkService.findByUrl(testUrl);
        assertNotNull(foundLink);
        assertEquals(testDescription, foundLink.getDescription());
        LOGGER.info("Link found: {}", foundLink);
    }

    @Test
    @Transactional
    void listAll_ReturnsAllLinks() {
        LOGGER.info("Running listAll_ReturnsAllLinks...");
        Collection<LinkDTO> allLinks = linkService.listAll();
        assertFalse(allLinks.isEmpty());
        assertTrue(allLinks.stream().anyMatch(link -> link.getUrl().equals(testUrl)));
        LOGGER.info("Found {} links", allLinks.size());
    }
}
