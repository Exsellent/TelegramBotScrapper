package backend.academy.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import backend.academy.bot.client.ScrapperApiClient;
import backend.academy.bot.dto.ListLinksResponse;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

class ScrapperApiClientTest {
    private ScrapperApiClient scrapperApiClient;
    private RestClient restClientMock;

    @BeforeEach
    void setUp() {
        restClientMock = mock(RestClient.class);
        scrapperApiClient = new ScrapperApiClient("http://localhost:8080");
    }

    @Test
    void testGetAllLinks_Success() {
        // Arrange
        ListLinksResponse mockResponse = new ListLinksResponse(Collections.emptyList());
        ScrapperApiClient spyClient = Mockito.spy(scrapperApiClient);
        doReturn(mockResponse).when(spyClient).getAllLinks(anyLong());

        // Act
        ListLinksResponse response = spyClient.getAllLinks(123L);

        // Assert
        assertNotNull(response);
        assertTrue(response.links().isEmpty());
    }
}
