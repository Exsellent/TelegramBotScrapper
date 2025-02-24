package backend.academy.scrapper.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CombinedStackOverflowInfo {

    private QuestionResponse question;
    private List<AnswerResponse> answers;
    private OffsetDateTime latestUpdate;

    public QuestionResponse getQuestion() {
        return question;
    }

    public List<AnswerResponse> getAnswers() {
        return answers;
    }

    public OffsetDateTime getLatestUpdate() {
        return latestUpdate;
    }

    public CombinedStackOverflowInfo(QuestionResponse question, List<AnswerResponse> answers) {
        this.question = question;
        this.answers = answers;
        this.latestUpdate = calculateLatestUpdate(question, answers);
    }

    private OffsetDateTime calculateLatestUpdate(QuestionResponse question, List<AnswerResponse> answers) {
        OffsetDateTime latestDate = question.getLastActivityDate();
        for (AnswerResponse answer : answers) {
            if (answer.getLastActivityDate().isAfter(latestDate)) {
                latestDate = answer.getLastActivityDate();
            }
        }
        return latestDate;
    }
}
