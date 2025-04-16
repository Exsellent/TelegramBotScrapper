package backend.academy.scrapper.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("MagicNumber")
public class StackOverflowLinkExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackOverflowLinkExtractor.class);
    private static final int QUESTION_ID_INDEX = 4;
    private static final String INVALID_URL_MESSAGE = "Invalid StackOverflow URL: ";

    private StackOverflowLinkExtractor() {}

    public static String extractQuestionId(String url) {
        try {
            String[] parts = url.split("/");
            if (parts.length > QUESTION_ID_INDEX && "questions".equals(parts[3])) {
                return parts[QUESTION_ID_INDEX];
            }
            throw new IllegalArgumentException(INVALID_URL_MESSAGE + url);
        } catch (ArrayIndexOutOfBoundsException e) {
            LOGGER.error("Failed to extract question ID from URL: {}", url, e);
            throw new IllegalArgumentException(INVALID_URL_MESSAGE + url, e);
        }
    }
}
