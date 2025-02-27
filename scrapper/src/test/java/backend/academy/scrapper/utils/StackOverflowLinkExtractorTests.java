package backend.academy.scrapper.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StackOverflowLinkExtractorTests {

    @Test
    public void testExtractQuestionId() {
        String url = "https://stackoverflow.com/questions/58549361/using-dotenv-files-with-spring-boot";
        Assertions.assertEquals("58549361", StackOverflowLinkExtractor.extractQuestionId(url));
    }

    @Test
    public void testInvalidStackOverflowUrl() {
        String url = "https://stackoverflow.com/not/questions";
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> StackOverflowLinkExtractor.extractQuestionId(url));
    }
}
