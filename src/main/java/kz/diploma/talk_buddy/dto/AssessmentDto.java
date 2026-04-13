package kz.diploma.talk_buddy.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssessmentDto {

    private List<AssessmentQuestionDto> mcq;
    private List<AssessmentQuestionDto> open;
    private List<AssessmentQuestionDto> speaking;
}