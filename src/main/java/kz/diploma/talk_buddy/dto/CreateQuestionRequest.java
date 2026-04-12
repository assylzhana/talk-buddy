package kz.diploma.talk_buddy.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateQuestionRequest {

    private String questionText;

    private String answer1;
    private String answer2;
    private String answer3;
    private String answer4;

    private Integer correctIndex;
}