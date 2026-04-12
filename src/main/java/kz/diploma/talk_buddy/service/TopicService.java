package kz.diploma.talk_buddy.service;

import kz.diploma.talk_buddy.dto.CreateQuestionRequest;
import kz.diploma.talk_buddy.dto.CreateTopicRequest;
import kz.diploma.talk_buddy.entity.Answer;
import kz.diploma.talk_buddy.entity.Level;
import kz.diploma.talk_buddy.entity.Question;
import kz.diploma.talk_buddy.entity.Topic;
import kz.diploma.talk_buddy.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    public long countByLevel(Level level) {
        return topicRepository.countByLevel(level);
    }

    private final TopicRepository topicRepository;

    public List<Topic> findAll() {
        return topicRepository.findAll();
    }

    public List<Topic> findByLevel(Level level) {
        return topicRepository.findByLevelOrderByIdAsc(level);
    }

    public Topic findById(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Тема не найдена"));
    }

    public Topic save(Topic topic) {
        return topicRepository.save(topic);
    }

    public void delete(Long id) {
        topicRepository.deleteById(id);
    }

    public Topic createTopicWithQuestions(CreateTopicRequest request) {
        Topic topic = new Topic();
        topic.setName(request.getName());
        topic.setDescription(request.getDescription());
        topic.setLevel(request.getLevel() == null || request.getLevel().isBlank()
                ? null
                : Level.valueOf(request.getLevel()));

        if (request.getQuestions() != null) {
            for (CreateQuestionRequest qReq : request.getQuestions()) {
                if (qReq.getQuestionText() == null || qReq.getQuestionText().isBlank()) {
                    continue;
                }

                Question question = new Question();
                question.setText(qReq.getQuestionText());
                question.setTopic(topic);

                question.getAnswers().add(buildAnswer(question, qReq.getAnswer1(), qReq.getCorrectIndex(), 0));
                question.getAnswers().add(buildAnswer(question, qReq.getAnswer2(), qReq.getCorrectIndex(), 1));
                question.getAnswers().add(buildAnswer(question, qReq.getAnswer3(), qReq.getCorrectIndex(), 2));
                question.getAnswers().add(buildAnswer(question, qReq.getAnswer4(), qReq.getCorrectIndex(), 3));

                topic.getQuestions().add(question);
            }
        }

        return topicRepository.save(topic);
    }

    private Answer buildAnswer(Question question, String text, Integer correctIndex, int currentIndex) {
        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setText(text);
        answer.setCorrect(correctIndex != null && correctIndex == currentIndex);
        return answer;
    }
}