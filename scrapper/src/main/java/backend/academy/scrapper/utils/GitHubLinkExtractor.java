package backend.academy.scrapper.utils;

@SuppressWarnings("MagicNumber")
public class GitHubLinkExtractor {

    private GitHubLinkExtractor() {}

    private static final String INVALID_GITHUB_URL = "Invalid GitHub URL: ";

    public static String extractOwner(String url) {
        String[] parts = url.split("/");

        if (parts.length > 3) {
            return parts[3];
        }
        throw new IllegalArgumentException(INVALID_GITHUB_URL + url);
    }

    public static String extractRepo(String url) {
        String[] parts = url.split("/");
        if (parts.length > 4) {
            return parts[4];
        }
        throw new IllegalArgumentException(INVALID_GITHUB_URL + url);
    }

    public static int extractPullRequestId(String url) {
        String[] parts = url.split("/");
        if (parts.length > 6 && "pull".equals(parts[5])) {
            try {
                return Integer.parseInt(parts[6]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Pull request ID is not a valid number in URL: " + url);
            }
        }
        throw new IllegalArgumentException(INVALID_GITHUB_URL + url);
    }

    public static int extractIssueId(String url) {
        String[] parts = url.split("/");
        if (parts.length > 6 && "issues".equals(parts[5])) {
            try {
                return Integer.parseInt(parts[6]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Issue ID is not a valid number in URL: " + url);
            }
        }
        throw new IllegalArgumentException(INVALID_GITHUB_URL + url);
    }
}
