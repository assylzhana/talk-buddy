package kz.diploma.talk_buddy.service;

import kz.diploma.talk_buddy.entity.Answer;
import kz.diploma.talk_buddy.repository.AnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;

    public List<Answer> findAll() {
        return answerRepository.findAll();
    }

    public List<Answer> findByQuestionId(Long questionId) {
        return answerRepository.findByQuestionId(questionId);
    }

    public Answer findById(Long id) {
        return answerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ответ не найден"));
    }

    public Answer save(Answer answer) {
        return answerRepository.save(answer);
    }

    public void delete(Long id) {
        answerRepository.deleteById(id);
    }
}