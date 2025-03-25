package backend.academy.scrapper.repository.repository.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CombinedPullRequestInfo {
    private String title;
    private PullRequestResponse pullRequest;
    private List<IssuesCommentsResponse> issueComments;
    private List<PullCommentsResponse> pullComments;
}
