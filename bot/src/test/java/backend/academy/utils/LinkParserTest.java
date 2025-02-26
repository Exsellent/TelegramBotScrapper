package backend.academy.utils;

import backend.academy.bot.utils.LinkParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkParserTest {

    @Test
    void whenUrlIsValid_thenTrueIsReturned() {
        assertTrue(LinkParser.isValidURL("http://example.com"));
        assertTrue(LinkParser.isValidURL("https://example.com"));
        assertTrue(LinkParser.isValidURL("https://www.example.com/path?query=123"));
    }

    @Test
    void whenUrlIsInvalid_thenFalseIsReturned() {
        assertFalse(LinkParser.isValidURL("example"));
        assertFalse(LinkParser.isValidURL("http://"));
        assertFalse(LinkParser.isValidURL("https://example.com:port"));
    }

    @Test
    void whenUrlIsNull_thenFalseIsReturned() {
        assertFalse(LinkParser.isValidURL(null));
    }
}
