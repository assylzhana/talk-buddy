package kz.diploma.talk_buddy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminQuestionForm {

    @NotNull
    private Long topicId;

    @NotBlank(message = "Текст вопроса обязателен")
    private String questionText;

    @NotBlank(message = "Ответ 1 обязателен")
    private String answer1;

    @NotBlank(message = "Ответ 2 обязателен")
    private String answer2;

    @NotBlank(message = "Ответ 3 обязателен")
    private String answer3;

    @NotBlank(message = "Ответ 4 обязателен")
    private String answer4;

    @NotNull(message = "Выбери правильный ответ")
    private Integer correctAnswerIndex;
}