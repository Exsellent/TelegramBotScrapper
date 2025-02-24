package backend.academy.scrapper.utils;

@SuppressWarnings("MagicNumber")
public class StackOverflowLinkExtractor {

    private static final int QUESTION_ID_INDEX = 4;

    private StackOverflowLinkExtractor() {}

    public static String extractQuestionId(String url) {
        String[] parts = url.split("/");
        if (parts.length > QUESTION_ID_INDEX && "questions".equals(parts[3])) {
            return parts[QUESTION_ID_INDEX];
        }
        throw new IllegalArgumentException("Invalid StackOverflow URL: " + url);
    }
}
