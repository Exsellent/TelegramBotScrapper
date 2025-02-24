package backend.academy.scrapper.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@ToString
public class CombinedPullRequestInfo {
    private String title;
    private List<IssuesCommentsResponse> issueComments;
    private List<PullCommentsResponse> pullComments;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<IssuesCommentsResponse> getIssueComments() {
        return issueComments;
    }

    public void setIssueComments(List<IssuesCommentsResponse> issueComments) {
        this.issueComments = issueComments;
    }

    public List<PullCommentsResponse> getPullComments() {
        return pullComments;
    }

    public void setPullComments(List<PullCommentsResponse> pullComments) {
        this.pullComments = pullComments;
    }
}
