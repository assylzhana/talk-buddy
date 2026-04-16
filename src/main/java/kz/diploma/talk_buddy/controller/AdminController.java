package kz.diploma.talk_buddy.controller;

import kz.diploma.talk_buddy.dto.CreateQuestionRequest;
import kz.diploma.talk_buddy.dto.CreateTopicRequest;
import kz.diploma.talk_buddy.dto.GenerateRequest;
import kz.diploma.talk_buddy.entity.*;
import kz.diploma.talk_buddy.repository.PhotoRepository;
import kz.diploma.talk_buddy.repository.QuestionRepository;
import kz.diploma.talk_buddy.repository.VideoRepository;
import kz.diploma.talk_buddy.repository.TopicProgressRepository;
import kz.diploma.talk_buddy.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TopicService topicService;
    private final QuestionService questionService;
    private final UserService userService;
    private final AiAssessmentService aiService;
    private final VideoRepository videoRepository;
    private final PhotoRepository photoRepository;
    private final QuestionRepository questionRepository;
    private final TopicProgressRepository topicProgressRepository;

    @GetMapping
    public String contentPage(Model model) {
        model.addAttribute("levels", Level.values());
        return "admin/index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {

        String username = authentication.getName();

        model.addAttribute("username", username);

        model.addAttribute("a1Count", topicService.countByLevel(Level.A1));
        model.addAttribute("a2Count", topicService.countByLevel(Level.A2));
        model.addAttribute("b1Count", topicService.countByLevel(Level.B1));
        model.addAttribute("b2Count", topicService.countByLevel(Level.B2));

        return "admin/dashboard";
    }

    @PostMapping("/topics/{id}/delete")
    public String deleteTopic(@PathVariable Long id,
                              @RequestParam(required = false) String level) {

        topicService.delete(id);

        if (level != null) {
            return "redirect:/admin/level/" + level;
        }

        return "redirect:/admin";
    }

    @GetMapping("/level/{level}")
    public String levelPage(@PathVariable Level level, Model model) {
        var topics = topicService.findByLevel(level);

        Map<Long, Long> passedCountMap = new HashMap<>();

        for (Topic topic : topics) {
            long count = topicProgressRepository.countStudentsByTopic(topic);
            passedCountMap.put(topic.getId(), count);
        }

        model.addAttribute("level", level);
        model.addAttribute("topics", topics);
        model.addAttribute("passedCountMap", passedCountMap);

        return "admin/level-topics";
    }

    @GetMapping("/level/{level}/students/{topicId}")
    public String testStudents(@PathVariable Level level,
                               @PathVariable Long topicId,
                               Model model) {

        Topic topic = topicService.findById(topicId);
        List<TopicProgress> progresses = topicProgressRepository.findByTopic(topic);

        model.addAttribute("level", level);
        model.addAttribute("topic", topic);
        model.addAttribute("progresses", progresses);

        return "admin/test-student";
    }

    @GetMapping("/topics/create")
    public String createTopicPage(@RequestParam Level level, Model model) {
        CreateTopicRequest topicRequest = new CreateTopicRequest();
        topicRequest.setLevel(level.name());

        CreateQuestionRequest firstQuestion = new CreateQuestionRequest();
        topicRequest.getQuestions().add(firstQuestion);

        model.addAttribute("topicRequest", topicRequest);
        model.addAttribute("level", level);
        return "admin/topic-create";
    }

    @PostMapping("/topics")
    public String createTopic(@ModelAttribute CreateTopicRequest topicRequest) {
        topicService.createTopicWithQuestions(topicRequest);
        System.out.println("NAME = " + topicRequest.getName());
        return "redirect:/admin/level/" + topicRequest.getLevel();
    }


    @GetMapping("/topics/{id}")
    public String editTopic(@PathVariable Long id, Model model) {
        model.addAttribute("topic", topicService.findById(id));
        return "admin/topic-edit";
    }

    @PostMapping("/topics/{id}/update")
    public String updateTopic(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam String description) {
        Topic topic = topicService.findById(id);
        topic.setName(name);
        topic.setDescription(description);
        topicService.save(topic);
        return "redirect:/admin/topics/" + id;
    }

    @PostMapping("/questions")
    public String addQuestion(@RequestParam Long topicId,
                              @RequestParam String text,
                              @RequestParam String type,
                              @RequestParam(required = false) String answer1,
                              @RequestParam(required = false) String answer2,
                              @RequestParam(required = false) String answer3,
                              @RequestParam(required = false) String answer4,
                              @RequestParam(required = false) Integer correctIndex,
                              @RequestParam(required = false) String correctAnswer,
                              @RequestParam(required = false) List<String> leftItems,
                              @RequestParam(required = false) List<String> rightItems) {

        Topic topic = topicService.findById(topicId);

        Question q = new Question();
        q.setText(text);
        q.setTopic(topic);
        q.setType(QuestionType.valueOf(type));
        q.setCorrectAnswer(null);

        if (q.getType() == QuestionType.TEST) {
            addAnswerIfNotBlank(q, answer1, correctIndex, 0);
            addAnswerIfNotBlank(q, answer2, correctIndex, 1);
            addAnswerIfNotBlank(q, answer3, correctIndex, 2);
            addAnswerIfNotBlank(q, answer4, correctIndex, 3);
        }

        if (q.getType() == QuestionType.FILL_GAP) {
            q.setCorrectAnswer(correctAnswer);
        }

        if (q.getType() == QuestionType.MATCHING && leftItems != null && rightItems != null) {
            int size = Math.min(leftItems.size(), rightItems.size());

            for (int i = 0; i < size; i++) {
                String left = leftItems.get(i);
                String right = rightItems.get(i);

                if (left == null || left.isBlank() || right == null || right.isBlank()) {
                    continue;
                }

                MatchingPair pair = new MatchingPair();
                pair.setLeftText(left);
                pair.setRightText(right);
                pair.setQuestion(q);

                q.getPairs().add(pair);
            }
        }

        questionService.save(q);

        return "redirect:/admin/topics/" + topicId;
    }

    private void addAnswerIfNotBlank(Question q, String text, Integer correctIndex, int index) {
        if (text == null || text.isBlank()) {
            return;
        }

        Answer a = new Answer();
        a.setText(text);
        a.setCorrect(correctIndex != null && correctIndex == index);
        a.setQuestion(q);

        q.getAnswers().add(a);
    }

    @PostMapping("/questions/{id}/update")
    public String updateQuestion(@PathVariable Long id,
                                 @RequestParam String text,
                                 @RequestParam String type,
                                 @RequestParam(required = false) String answer1,
                                 @RequestParam(required = false) String answer2,
                                 @RequestParam(required = false) String answer3,
                                 @RequestParam(required = false) String answer4,
                                 @RequestParam(required = false) Integer correctIndex,
                                 @RequestParam(required = false) String correctAnswer,
                                 @RequestParam(required = false) List<String> leftItems,
                                 @RequestParam(required = false) List<String> rightItems) {

        Question question = questionService.findById(id);

        question.setText(text);
        question.setType(QuestionType.valueOf(type));
        question.setCorrectAnswer(null);

        question.getAnswers().clear();
        question.getPairs().clear();

        if (question.getType() == QuestionType.TEST) {
            addAnswerIfNotBlank(question, answer1, correctIndex, 0);
            addAnswerIfNotBlank(question, answer2, correctIndex, 1);
            addAnswerIfNotBlank(question, answer3, correctIndex, 2);
            addAnswerIfNotBlank(question, answer4, correctIndex, 3);
        }

        if (question.getType() == QuestionType.FILL_GAP) {
            question.setCorrectAnswer(correctAnswer);
        }

        if (question.getType() == QuestionType.MATCHING && leftItems != null && rightItems != null) {
            int size = Math.min(leftItems.size(), rightItems.size());

            for (int i = 0; i < size; i++) {
                String left = leftItems.get(i);
                String right = rightItems.get(i);

                if (left == null || left.isBlank() || right == null || right.isBlank()) {
                    continue;
                }

                MatchingPair pair = new MatchingPair();
                pair.setLeftText(left);
                pair.setRightText(right);
                pair.setQuestion(question);

                question.getPairs().add(pair);
            }
        }

        questionService.save(question);

        return "redirect:/admin/topics/" + question.getTopic().getId();
    }

    @PostMapping("/questions/{id}/delete")
    public String deleteQuestion(@PathVariable Long id) {
        Question question = questionService.findById(id);
        Long topicId = question.getTopic().getId();
        questionService.delete(id);
        return "redirect:/admin/topics/" + topicId;
    }

    @GetMapping("/students")
    public String studentsPage(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("students", userService.findStudents(search));
        model.addAttribute("search", search == null ? "" : search);
        return "admin/students";
    }
    @PostMapping("/videos/{id}/delete")
    public String deleteVideo(@PathVariable Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        Long topicId = video.getTopic().getId();

        videoRepository.delete(video);

        return "redirect:/admin/topics/" + topicId;
    }
    @PostMapping("/photos/{id}/delete")
    public String deletePhoto(@PathVariable Long id) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        Long topicId = photo.getTopic().getId();

        photoRepository.delete(photo);

        return "redirect:/admin/topics/" + topicId;
    }

    @PostMapping("/videos")
    public String addVideo(@RequestParam Long topicId,
                           @RequestParam String url) {

        Topic topic = topicService.findById(topicId);

        Video video = new Video();
        video.setUrl(url);
        video.setTopic(topic);

        videoRepository.save(video);

        return "redirect:/admin/topics/" + topicId;
    }

    @PostMapping("/photos")
    public String addPhoto(@RequestParam Long topicId,
                           @RequestParam String url) {

        Topic topic = topicService.findById(topicId);

        Photo photo = new Photo();
        photo.setUrl(url);
        photo.setTopic(topic);

        photoRepository.save(photo);

        return "redirect:/admin/topics/" + topicId;
    }

    @PostMapping("/generate-questions")
    @ResponseBody
    public String generate(@RequestBody GenerateRequest request) {

        Topic topic = topicService.findById(request.getTopicId());

        request.setLevel(topic.getLevel().name());
        List<CreateQuestionRequest> generated = null;
        try {
            generated = aiService.generateQuestions(request);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        List<Question> questions = new ArrayList<>();

        for (CreateQuestionRequest dto : generated) {

            Question q = new Question();
            q.setText(dto.getQuestionText());
            q.setType(QuestionType.valueOf(dto.getType()));

            q.setTopic(topic);

            if ("TEST".equals(dto.getType())) {
                List<Answer> answers = new ArrayList<>();

                for (int i = 0; i < dto.getAnswers().size(); i++) {
                    Answer a = new Answer();
                    a.setText(dto.getAnswers().get(i));
                    a.setCorrect(i == dto.getCorrectIndex());
                    a.setQuestion(q);
                    answers.add(a);
                }

                q.setAnswers(answers);
            }

            if ("FILL_GAP".equals(dto.getType())) {
                q.setCorrectAnswer(dto.getCorrectAnswer());
            }

            questions.add(q);
        }

        questionRepository.saveAll(questions);

        return "ok";
    }

}