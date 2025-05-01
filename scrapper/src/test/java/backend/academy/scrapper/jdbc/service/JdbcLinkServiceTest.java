package backend.academy.scrapper.jdbc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.dao.LinkDao;
import backend.academy.scrapper.database.jdbc.service.JdbcLinkService;
import backend.academy.scrapper.dto.LinkDTO;
import backend.academy.scrapper.exception.LinkAlreadyAddedException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JdbcLinkServiceTest {

    @Mock
    private LinkDao linkDao;

    @InjectMocks
    private JdbcLinkService linkService;

    private final String testUrl = "http://example.com/test";
    private final String testDescription = "Test Description";
    private final Long testLinkId = 1L;
    private LinkDTO testLink;

    @BeforeEach
    void setUp() {
        testLink = LinkDTO.builder()
                .linkId(testLinkId)
                .url(testUrl)
                .description(testDescription)
                .createdAt(LocalDateTime.now())
                .lastCheckTime(LocalDateTime.now())
                .lastUpdateTime(LocalDateTime.now())
                .build();
        lenient().when(linkDao.existsByUrl(testUrl)).thenReturn(false);
    }

    @Test
    void add_ThrowsLinkAlreadyAddedException_WhenLinkExists() {
        when(linkDao.existsByUrl(testUrl)).thenReturn(true);

        Exception exception =
                assertThrows(LinkAlreadyAddedException.class, () -> linkService.add(testUrl, testDescription));
        assertTrue(exception.getMessage().contains(testUrl + " already exists."));
        verify(linkDao).existsByUrl(testUrl);
        verify(linkDao, never()).add(any());
    }

    @Test
    void remove_ThrowsLinkNotFoundException_WhenLinkNotExists() {
        String nonExistentUrl = "http://nonexistent.com";
        when(linkDao.existsByUrl(nonExistentUrl)).thenReturn(false);

        Exception exception = assertThrows(LinkNotFoundException.class, () -> linkService.remove(nonExistentUrl));
        assertTrue(exception.getMessage().contains(nonExistentUrl + " not found."));
        verify(linkDao).existsByUrl(nonExistentUrl);
        verify(linkDao, never()).remove(anyString());
    }

    @Test
    void update_UpdatesLinkSuccessfully() {
        String updatedDescription = "Updated Description";
        LinkDTO updatedLink = LinkDTO.builder()
                .linkId(testLinkId)
                .url(testUrl)
                .description(updatedDescription)
                .createdAt(LocalDateTime.now())
                .lastCheckTime(LocalDateTime.now())
                .lastUpdateTime(LocalDateTime.now())
                .build();
        when(linkDao.existsByUrl(testUrl)).thenReturn(true);
        doNothing().when(linkDao).update(any(LinkDTO.class));
        when(linkDao.findById(testLinkId)).thenReturn(updatedLink);

        linkService.update(updatedLink);
        LinkDTO foundLink = linkService.findById(testLinkId);

        assertEquals(updatedDescription, foundLink.getDescription());
        verify(linkDao).existsByUrl(testUrl);
        verify(linkDao).update(updatedLink);
        verify(linkDao).findById(testLinkId);
    }

    @Test
    void findById_ReturnsCorrectLink() {
        when(linkDao.findById(testLinkId)).thenReturn(testLink);

        LinkDTO foundLink = linkService.findById(testLinkId);

        assertNotNull(foundLink);
        assertEquals(testUrl, foundLink.getUrl());
        assertEquals(testDescription, foundLink.getDescription());
        verify(linkDao).findById(testLinkId);
    }

    @Test
    void findByUrl_ReturnsCorrectLink() {
        when(linkDao.findByUrl(testUrl)).thenReturn(testLink);

        LinkDTO foundLink = linkService.findByUrl(testUrl);

        assertNotNull(foundLink);
        assertEquals(testDescription, foundLink.getDescription());
        verify(linkDao).findByUrl(testUrl);
    }

    @Test
    void listAll_ReturnsAllLinks() {
        when(linkDao.findAll()).thenReturn(List.of(testLink));

        Collection<LinkDTO> allLinks = linkService.listAll();

        assertFalse(allLinks.isEmpty());
        assertTrue(allLinks.stream().anyMatch(link -> link.getUrl().equals(testUrl)));
        verify(linkDao).findAll();
    }

    @Test
    void findLinksToCheck_ReturnsLinksNotCheckedSince() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        when(linkDao.findLinksNotCheckedSince(threshold, 0, 10)).thenReturn(List.of(testLink));

        Collection<LinkDTO> linksToCheck = linkService.findLinksToCheck(threshold, 0, 10);

        assertFalse(linksToCheck.isEmpty());
        verify(linkDao).findLinksNotCheckedSince(threshold, 0, 10);
    }
}
