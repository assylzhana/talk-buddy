package kz.diploma.talk_buddy.dto;

import java.util.Map;

public class TestSubmissionDto {
    private Map<Long, Long> selectedAnswers;

    public Map<Long, Long> getSelectedAnswers() {
        return selectedAnswers;
    }

    public void setSelectedAnswers(Map<Long, Long> selectedAnswers) {
        this.selectedAnswers = selectedAnswers;
    }
}