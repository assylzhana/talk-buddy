package kz.diploma.talk_buddy.service;

import kz.diploma.talk_buddy.dto.CreateQuestionRequest;
import kz.diploma.talk_buddy.dto.CreateTopicRequest;
import kz.diploma.talk_buddy.entity.*;
import kz.diploma.talk_buddy.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    public long countByLevel(Level level) {
        return topicRepository.countByLevel(level);
    }

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

                QuestionType type = qReq.getType() == null
                        ? QuestionType.TEST
                        : QuestionType.valueOf(qReq.getType());

                question.setType(type);

                if (type == QuestionType.TEST) {
                    question.getAnswers().add(buildAnswer(question, qReq.getAnswer1(), qReq.getCorrectIndex(), 0));
                    question.getAnswers().add(buildAnswer(question, qReq.getAnswer2(), qReq.getCorrectIndex(), 1));
                    question.getAnswers().add(buildAnswer(question, qReq.getAnswer3(), qReq.getCorrectIndex(), 2));
                    question.getAnswers().add(buildAnswer(question, qReq.getAnswer4(), qReq.getCorrectIndex(), 3));
                }

                if (type == QuestionType.FILL_GAP) {
                    question.setCorrectAnswer(qReq.getCorrectAnswer());
                }

                if (type == QuestionType.MATCHING) {
                    if (qReq.getLeftItems() != null && qReq.getRightItems() != null) {
                        for (int i = 0; i < qReq.getLeftItems().size(); i++) {
                            String left = qReq.getLeftItems().get(i);
                            String right = qReq.getRightItems().get(i);

                            if (left == null || right == null) continue;

                            MatchingPair pair = new MatchingPair();
                            pair.setLeftText(left);
                            pair.setRightText(right);
                            pair.setQuestion(question);

                            question.getPairs().add(pair);
                        }
                    }
                }

                topic.getQuestions().add(question);
            }
        }

        if (request.getVideoUrls() != null) {
            for (String url : request.getVideoUrls()) {
                if (url == null || url.isBlank()) continue;

                Video video = new Video();
                video.setUrl(url);
                video.setTopic(topic);

                topic.getVideos().add(video);
            }
        }

        if (request.getPhotoUrls() != null) {
            for (String url : request.getPhotoUrls()) {
                if (url == null || url.isBlank()) continue;

                Photo photo = new Photo();
                photo.setUrl(url);
                photo.setTopic(topic);

                topic.getPhotos().add(photo);
            }
        }

        return topicRepository.save(topic);
    }

    private Answer buildAnswer(Question question, String text, Integer correctIndex, int currentIndex) {

        if (text == null || text.isBlank()) {
            return null;
        }
        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setText(text);
        answer.setCorrect(correctIndex != null && correctIndex == currentIndex);
        return answer;
    }
}