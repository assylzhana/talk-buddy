package kz.diploma.talk_buddy.controller;

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

import java.util.HashMap;
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

    @PostMapping("/lesson/{id}/test")
    @ResponseBody
    public Map<String, Object> submitTest(@PathVariable Long id,
                                          @RequestParam Map<String, String> answers) {

        Topic topic = topicService.findById(id);

        int correct = 0;
        int total = 0;

        for (Question q : topic.getQuestions()) {

            total++;

            if (q.getType().name().equals("TEST")) {

                String userAnswer = answers.get("selectedAnswers[" + q.getId() + "]");

                for (Answer a : q.getAnswers()) {
                    if (a.isCorrect() &&
                            String.valueOf(a.getId()).equals(userAnswer)) {
                        correct++;
                    }
                }
            }

            if (q.getType().name().equals("FILL_GAP")) {

                String userAnswer = answers.get("fill_" + q.getId());

                if (userAnswer != null &&
                        userAnswer.equalsIgnoreCase(q.getCorrectAnswer())) {
                    correct++;
                }
            }

            if (q.getType().name().equals("MATCHING")) {

                boolean allCorrect = true;

                for (int i = 0; i < q.getPairs().size(); i++) {

                    MatchingPair pair = q.getPairs().get(i);

                    String userAnswer = answers.get("q_" + q.getId() + "_pair_" + i);

                    if (userAnswer == null ||
                            !userAnswer.equals(String.valueOf(pair.getId()))) {
                        allCorrect = false;
                    }
                }

                if (allCorrect) correct++;
            }
        }

        int percent = (int) ((correct * 100.0) / total);
        boolean passed = percent >= 75;

        User user = userRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        TopicProgress progress = topicProgressRepository
                .findTopByUserAndTopicOrderByIdDesc(user, topic);

        if (progress == null) {
            progress = new TopicProgress();
            progress.setUser(user);
            progress.setTopic(topic);
        }

        progress.setScore(correct);
        progress.setTotal(total);
        progress.setPercent(percent);
        progress.setPassed(passed);

        topicProgressRepository.save(progress);

        return Map.of(
                "correct", correct,
                "total", total,
                "percent", percent,
                "passed", passed
        );
    }
}