package kz.diploma.talk_buddy.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateTopicRequest {

    private String name;
    private String description;
    private String level;
    private List<String> videoUrls;
    private List<String> photoUrls;
    private List<CreateQuestionRequest> questions = new ArrayList<>();
}