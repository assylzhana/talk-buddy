package kz.diploma.talk_buddy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import kz.diploma.talk_buddy.dto.AssessmentDto;
import kz.diploma.talk_buddy.dto.AssessmentResultDto;
import kz.diploma.talk_buddy.dto.AssessmentSubmissionDto;
import kz.diploma.talk_buddy.entity.*;
import kz.diploma.talk_buddy.repository.TopicProgressRepository;
import kz.diploma.talk_buddy.repository.UserRepository;
import kz.diploma.talk_buddy.service.AiAssessmentService;
import kz.diploma.talk_buddy.service.QuestionService;
import kz.diploma.talk_buddy.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final TopicService topicService;
    private final QuestionService questionService;
    private final UserRepository userRepository;
    private final AiAssessmentService aiService;
    private final TopicProgressRepository topicProgressRepository;
    private final AiAssessmentService aiAssessmentService;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @GetMapping("/dashboard")
    public String dashboard(Model model,  HttpSession session) {
        User user = userRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        if (user.getLevel() == null) {

            AssessmentDto assessment =
                    (AssessmentDto) session.getAttribute("assessment");

            if (assessment == null) {
                assessment = aiService.generateAssessment();
                session.setAttribute("assessment", assessment);
            }

            model.addAttribute("assessment", assessment);
        }

        model.addAttribute("user", user);
        return "student/dashboard";
    }

    @PostMapping("/assessment/submit")
    public String submit(AssessmentSubmissionDto submission,
                         HttpSession session,
                         Model model) {

        User user = userRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        AssessmentResultDto result = aiService.evaluate(submission);

        user.setLevel(result.getLevel());
        userRepository.save(user);
        session.removeAttribute("assessment");

        model.addAttribute("user", user);
        model.addAttribute("result", result);

        return "student/dashboard";
    }

    @PostMapping("/update")
    public String updateUser(@RequestParam String firstName,
                             @RequestParam String lastName,
                             @RequestParam String email,
                             @RequestParam(required = false) String password) {

        User user = userRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);

        if (password != null && !password.isBlank()) {
            user.setPassword(password);
        }

        userRepository.save(user);

        return "redirect:/student/account";
    }
    @GetMapping("/account")
    public String account(Model model) {
        User user = userRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        model.addAttribute("user", user);
        return "student/account";
    }

    @GetMapping("/edit")
    public String edit(Model model) {
        User user = userRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        model.addAttribute("user", user);
        return "student/edit";
    }

    @GetMapping("/lesson")
    public String lessonPage(Model model) {

        User user = userRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Level level = Level.valueOf(user.getLevel());

        var topics = topicService.findByLevel(level);

        Map<Long, TopicProgress> progressMap = new HashMap<>();

        boolean allPassed = true;

        for (Topic t : topics) {

            TopicProgress p = topicProgressRepository
                    .findTopByUserAndTopicOrderByIdDesc(user, t);

            progressMap.put(t.getId(), p);

            if (p == null || !p.isPassed()) {
                allPassed = false;
            }
        }

        boolean showNextLevel = allPassed && level != Level.B2;

        model.addAttribute("topics", topics);
        model.addAttribute("progressMap", progressMap);
        model.addAttribute("showNextLevel", showNextLevel);
        model.addAttribute("level", level);

        return "student/lesson";
    }

    @PostMapping("/next-level")
    public String nextLevel() {

        User user = userRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Level current = Level.valueOf(user.getLevel());

        Level next = switch (current) {
            case A1 -> Level.A2;
            case A2 -> Level.B1;
            case B1 -> Level.B2;
            default -> current;
        };

        user.setLevel(next.name());
        userRepository.save(user);

        return "redirect:/student/lesson";
    }

    @GetMapping("/lesson/{id}")
    public String lessonDetails(@PathVariable Long id, Model model) {

        model.addAttribute("topic", topicService.findById(id));

        return "student/lesson-details";
    }

    @GetMapping("/lesson/{id}/test")
    public String testPage(@PathVariable Long id, Model model) {

        model.addAttribute("topic", topicService.findById(id));

        return "student/test";
    }

    @GetMapping("/lesson/{id}/chatbot")
    public String chatPage(@PathVariable Long id, Model model) {

        Topic topic = topicService.findById(id);
        model.addAttribute("topic", topic);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        topicProgressRepository.findByUserAndTopic(user, topic)
                .ifPresent(p -> {
                    model.addAttribute("progressPercent", p.getPercent());
                    model.addAttribute("hasMistakes",
                            p.getWrongQuestionsJson() != null && !p.getWrongQuestionsJson().equals("[]"));
                });

        return "student/chatbot";
    }

    @PostMapping("/lesson/{id}/chatbot")
    @ResponseBody
    public Map<String, String> chat(@PathVariable Long id,
                                    @RequestBody Map<String, String> req) {

        Topic topic = topicService.findById(id);
        String message = req.get("message");
        String mode = req.getOrDefault("mode", "learning");

        String reply;
        if ("correction".equals(mode)) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User chatUser = userRepository.findByUsername(username).orElseThrow();
            String level = chatUser.getLevel() != null ? chatUser.getLevel() : "B1";

            TopicProgress progress = topicProgressRepository
                    .findByUserAndTopic(chatUser, topic)
                    .orElse(null);

            String wrongJson = (progress != null && progress.getWrongQuestionsJson() != null)
                    ? progress.getWrongQuestionsJson() : "[]";
            int pct = progress != null ? progress.getPercent() : 0;

            reply = aiAssessmentService.reviewMistakes(wrongJson, pct, topic.getName(), level, message);
        } else {
            reply = aiAssessmentService.chatWithTopic(
                    topic.getName(),
                    topic.getDescription(),
                    message
            );
        }

        return Map.of("reply", reply);
    }

    @PostMapping("/lesson/{id}/test")
    public String submitTest(@PathVariable Long id,
                             @RequestParam Map<String, String> answers,
                             Model model) {

        Topic topic = topicService.findById(id);


        Map<Long, Boolean> resultMap = new HashMap<>();
        List<Map<String, String>> wrongQuestions = new ArrayList<>();

        int correct = 0;
        int total = 0;

        for (Question q : topic.getQuestions()) {

            total++;
            boolean isCorrect = false;
            String studentAnswer = null;
            String correctAnswer = null;

            if (q.getType().name().equals("TEST")) {

                String userAnswer = answers.get("selectedAnswers[" + q.getId() + "]");

                for (Answer a : q.getAnswers()) {
                    if (a.isCorrect()) correctAnswer = a.getText();
                    if (a.isCorrect() && String.valueOf(a.getId()).equals(userAnswer)) {
                        isCorrect = true;
                    }
                    if (String.valueOf(a.getId()).equals(userAnswer)) {
                        studentAnswer = a.getText();
                    }
                }
            }

            if (q.getType().name().equals("FILL_GAP")) {

                studentAnswer = answers.get("fill_" + q.getId());
                correctAnswer = q.getCorrectAnswer();

                if (studentAnswer != null && studentAnswer.equalsIgnoreCase(correctAnswer)) {
                    isCorrect = true;
                }
            }

            if (q.getType().name().equals("MATCHING")) {

                boolean allCorrect = true;
                StringBuilder correctPairs = new StringBuilder();
                StringBuilder studentPairs = new StringBuilder();

                for (int i = 0; i < q.getPairs().size(); i++) {
                    MatchingPair pair = q.getPairs().get(i);
                    String userAnswer = answers.get("q_" + q.getId() + "_pair_" + i);
                    if (userAnswer == null || !userAnswer.equals(String.valueOf(pair.getId()))) {
                        allCorrect = false;
                    }
                    correctPairs.append(pair.getLeftText()).append(" → ").append(pair.getRightText()).append("; ");
                }

                isCorrect = allCorrect;
                correctAnswer = correctPairs.toString();
                studentAnswer = isCorrect ? "correct" : "incorrect order";
            }

            if (isCorrect) {
                correct++;
            } else {
                Map<String, String> wrongEntry = new HashMap<>();
                wrongEntry.put("type", q.getType().name());
                wrongEntry.put("question", q.getText());
                wrongEntry.put("correct", correctAnswer != null ? correctAnswer : "");
                wrongEntry.put("student", studentAnswer != null ? studentAnswer : "(no answer)");
                wrongQuestions.add(wrongEntry);
            }

            resultMap.put(q.getId(), isCorrect);
        }

        int percent = (int) ((correct * 100.0) / total);
        boolean passed = percent >= 75;

        User user = userRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        TopicProgress progress = topicProgressRepository
                .findByUserAndTopic(user, topic)
                .orElse(new TopicProgress());

        progress.setUser(user);
        progress.setTopic(topic);
        progress.setScore(correct);
        progress.setTotal(total);
        progress.setPercent(percent);
        progress.setPassed(passed);

        try {
            progress.setWrongQuestionsJson(objectMapper.writeValueAsString(wrongQuestions));
        } catch (Exception ignored) {}

        topicProgressRepository.save(progress);
        model.addAttribute("topic", topic);
        model.addAttribute("resultMap", resultMap);
        model.addAttribute("correct", correct);
        model.addAttribute("total", total);
        model.addAttribute("percent", percent);
        model.addAttribute("passed", passed);

        return "student/test-result";
    }
}