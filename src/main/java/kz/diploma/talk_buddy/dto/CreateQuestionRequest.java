package kz.diploma.talk_buddy.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateQuestionRequest {

    private String questionText;

    private String answer1;
    private String answer2;
    private String answer3;
    private String answer4;

    private Integer correctIndex;

    private String type;

    // fill gap
    private String correctAnswer;

    // matching
    private List<String> leftItems;
    private List<String> rightItems;


    private List<String> answers;
}