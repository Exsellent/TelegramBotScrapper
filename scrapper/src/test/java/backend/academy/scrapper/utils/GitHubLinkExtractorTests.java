package backend.academy.scrapper.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GitHubLinkExtractorTests {

    @Test
    public void testExtractOwner() {
        String url = "https://github.com/Exsellent/ReflectionBenchmark";
        Assertions.assertEquals("Exsellent", GitHubLinkExtractor.extractOwner(url));
    }

    @Test
    public void testExtractRepo() {
        String url = "https://github.com/Exsellent/ReflectionBenchmark";
        Assertions.assertEquals("ReflectionBenchmark", GitHubLinkExtractor.extractRepo(url));
    }

    @Test
    public void testInvalidGitHubUrl() {
        String url1 = "https://github.com/";
        Assertions.assertThrows(IllegalArgumentException.class, () -> GitHubLinkExtractor.extractOwner(url1));
        String url2 = "https://github.com/Exsellent";
        Assertions.assertThrows(IllegalArgumentException.class, () -> GitHubLinkExtractor.extractRepo(url2));
        String url3 = "https://github.com/Exsellent/ReflectionBenchmark";
        Assertions.assertThrows(IllegalArgumentException.class, () -> GitHubLinkExtractor.extractPullRequestId(url3));
    }
}
