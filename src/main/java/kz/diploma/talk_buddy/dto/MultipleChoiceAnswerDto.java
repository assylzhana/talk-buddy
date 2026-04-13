package kz.diploma.talk_buddy.dto;

import lombok.Data;

@Data
public class MultipleChoiceAnswerDto {

    private String questionId;
    private String selectedOption;
}