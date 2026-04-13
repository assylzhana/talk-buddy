package kz.diploma.talk_buddy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssessmentResultDto {

    private String level;

    private int multipleChoiceScore;
    private int openQuestionScore;
    private int listeningScore;

    private int totalScore;

    private String feedback;
}