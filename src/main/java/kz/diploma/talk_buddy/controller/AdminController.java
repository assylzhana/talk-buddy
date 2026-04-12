package kz.diploma.talk_buddy.controller;

import kz.diploma.talk_buddy.dto.CreateQuestionRequest;
import kz.diploma.talk_buddy.dto.CreateTopicRequest;
import kz.diploma.talk_buddy.entity.*;
import kz.diploma.talk_buddy.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TopicService topicService;
    private final QuestionService questionService;
    private final UserService userService;
    private final AnswerService answerService;

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
        model.addAttribute("level", level);
        model.addAttribute("topics", topicService.findByLevel(level));
        return "admin/level-topics";
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
                              @RequestParam String answer1,
                              @RequestParam String answer2,
                              @RequestParam String answer3,
                              @RequestParam String answer4,
                              @RequestParam int correctIndex) {

        Topic topic = topicService.findById(topicId);

        Question q = new Question();
        q.setText(text);
        q.setTopic(topic);

        Answer a1 = new Answer();
        a1.setText(answer1);
        a1.setCorrect(correctIndex == 0);
        a1.setQuestion(q);

        Answer a2 = new Answer();
        a2.setText(answer2);
        a2.setCorrect(correctIndex == 1);
        a2.setQuestion(q);

        Answer a3 = new Answer();
        a3.setText(answer3);
        a3.setCorrect(correctIndex == 2);
        a3.setQuestion(q);

        Answer a4 = new Answer();
        a4.setText(answer4);
        a4.setCorrect(correctIndex == 3);
        a4.setQuestion(q);

        q.getAnswers().add(a1);
        q.getAnswers().add(a2);
        q.getAnswers().add(a3);
        q.getAnswers().add(a4);

        questionService.save(q);

        return "redirect:/admin/topics/" + topicId;
    }

    @PostMapping("/questions/{id}/update")
    public String updateQuestion(@PathVariable Long id,
                                 @RequestParam String text,
                                 @RequestParam String answer1,
                                 @RequestParam String answer2,
                                 @RequestParam String answer3,
                                 @RequestParam String answer4,
                                 @RequestParam int correctIndex) {

        Question question = questionService.findById(id);
        question.setText(text);

        if (question.getAnswers().size() >= 4) {
            question.getAnswers().get(0).setText(answer1);
            question.getAnswers().get(0).setCorrect(correctIndex == 0);

            question.getAnswers().get(1).setText(answer2);
            question.getAnswers().get(1).setCorrect(correctIndex == 1);

            question.getAnswers().get(2).setText(answer3);
            question.getAnswers().get(2).setCorrect(correctIndex == 2);

            question.getAnswers().get(3).setText(answer4);
            question.getAnswers().get(3).setCorrect(correctIndex == 3);
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

}