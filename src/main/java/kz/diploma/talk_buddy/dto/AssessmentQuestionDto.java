package kz.diploma.talk_buddy.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssessmentQuestionDto {

    private String id;
    private String type; // MCQ, OPEN, LISTENING
    private String question;
    private List<String> options;

    private String script;
    private String audioText;
    private String audioUrl;
    private String correctAnswer;
}