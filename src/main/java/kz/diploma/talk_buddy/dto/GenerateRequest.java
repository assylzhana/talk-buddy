package kz.diploma.talk_buddy.dto;

import lombok.Data;

@Data
public class GenerateRequest {
    private int count;
    private String topic;
    private String description;
    private String level;
    private Long topicId;
}