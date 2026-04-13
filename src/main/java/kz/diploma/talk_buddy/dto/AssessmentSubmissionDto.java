package kz.diploma.talk_buddy.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssessmentSubmissionDto {

    private List<MultipleChoiceAnswerDto> multipleChoiceAnswers;
    private List<OpenAnswerDto> openAnswers;
    private List<SpeakingAnswerDto> speakingAnswers;
}