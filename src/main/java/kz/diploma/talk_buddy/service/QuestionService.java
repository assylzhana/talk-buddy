package kz.diploma.talk_buddy.service;

import kz.diploma.talk_buddy.dto.AdminQuestionForm;
import kz.diploma.talk_buddy.entity.Answer;
import kz.diploma.talk_buddy.entity.Question;
import kz.diploma.talk_buddy.entity.Topic;
import kz.diploma.talk_buddy.repository.QuestionRepository;
import kz.diploma.talk_buddy.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final TopicService topicService;

    public Question findById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Вопрос не найден"));
    }

    public List<Question> findAll() {
        return questionRepository.findAll();
    }

    public Question save(Question question) {
        return questionRepository.save(question);
    }
    public void addQuestion(Long topicId, String text,
                            String a1, String a2, String a3, String a4,
                            int correctIndex) {

        Topic topic = topicRepository.findById(topicId).orElseThrow();

        Question q = new Question();
        q.setText(text);
        q.setTopic(topic);

        q.getAnswers().add(new Answer(null, a1, correctIndex == 0, q));
        q.getAnswers().add(new Answer(null, a2, correctIndex == 1, q));
        q.getAnswers().add(new Answer(null, a3, correctIndex == 2, q));
        q.getAnswers().add(new Answer(null, a4, correctIndex == 3, q));

        questionRepository.save(q);
    }

    public void delete(Long id) {
        questionRepository.deleteById(id);
    }

    public void createWithAnswers(AdminQuestionForm form) {
        Topic topic = topicService.findById(form.getTopicId());

        Question question = new Question();
        question.setText(form.getQuestionText());
        question.setTopic(topic);

        question.getAnswers().add(buildAnswer(question, form.getAnswer1(), form.getCorrectAnswerIndex() == 0));
        question.getAnswers().add(buildAnswer(question, form.getAnswer2(), form.getCorrectAnswerIndex() == 1));
        question.getAnswers().add(buildAnswer(question, form.getAnswer3(), form.getCorrectAnswerIndex() == 2));
        question.getAnswers().add(buildAnswer(question, form.getAnswer4(), form.getCorrectAnswerIndex() == 3));

        questionRepository.save(question);
    }

    private Answer buildAnswer(Question question, String text, boolean isCorrect) {
        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setText(text);
        answer.setCorrect(isCorrect);
        return answer;
    }
}