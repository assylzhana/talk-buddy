package kz.diploma.talk_buddy.dto;

import lombok.Data;

@Data
public class SpeakingAnswerDto {
    private String questionId;
    private String audioBase64;
}