package backend.academy.scrapper.utils;

@SuppressWarnings("MagicNumber")
public class GitHubLinkExtractor {

    private GitHubLinkExtractor() {}

    private static final String INVALID_GITHUB_URL = "Invalid GitHub URL: ";

    private static final int OWNER_INDEX = 3;
    private static final int REPO_INDEX = 4;
    private static final int TYPE_INDEX = 5;
    private static final int ID_INDEX = 6;

    public static String extractOwner(String url) {
        String[] parts = url.split("/");
        if (parts.length > OWNER_INDEX) {
            return parts[OWNER_INDEX];
        }
        throw new IllegalArgumentException(INVALID_GITHUB_URL + url);
    }

    public static String extractRepo(String url) {
        String[] parts = url.split("/");
        if (parts.length > REPO_INDEX) {
            return parts[REPO_INDEX];
        }
        throw new IllegalArgumentException(INVALID_GITHUB_URL + url);
    }

    public static int extractPullRequestId(String url) {
        String[] parts = url.split("/");
        if (parts.length > ID_INDEX && "pull".equals(parts[TYPE_INDEX])) {
            try {
                return Integer.parseInt(parts[ID_INDEX]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Pull request ID is not a valid number in URL: " + url);
            }
        }
        throw new IllegalArgumentException(INVALID_GITHUB_URL + url);
    }

    public static int extractIssueId(String url) {
        String[] parts = url.split("/");
        if (parts.length > ID_INDEX && "issues".equals(parts[TYPE_INDEX])) {
            try {
                return Integer.parseInt(parts[ID_INDEX]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Issue ID is not a valid number in URL: " + url);
            }
        }
        throw new IllegalArgumentException(INVALID_GITHUB_URL + url);
    }
}
