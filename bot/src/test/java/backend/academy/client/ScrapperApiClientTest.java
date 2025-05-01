package backend.academy.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        ListLinksResponse mockResponse = mock(ListLinksResponse.class);
        when(mockResponse.getLinks()).thenReturn(Collections.emptyList());
        ScrapperApiClient spyClient = Mockito.spy(scrapperApiClient);
        doReturn(mockResponse).when(spyClient).getAllLinks(anyLong());

        // Act
        ListLinksResponse response = spyClient.getAllLinks(123L);

        // Assert
        assertNotNull(response);
        assertTrue(response.getLinks().isEmpty());
    }
}
