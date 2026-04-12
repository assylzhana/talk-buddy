package kz.diploma.talk_buddy.dto;

import jakarta.validation.constraints.NotBlank;
import kz.diploma.talk_buddy.entity.Level;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminTopicForm {

    @NotBlank(message = "Название темы обязательно")
    private String name;

    @NotBlank(message = "Описание темы обязательно")
    private String description;

    private Level level;
}