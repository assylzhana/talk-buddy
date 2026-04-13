package kz.diploma.talk_buddy.controller;

import jakarta.servlet.http.HttpSession;
import kz.diploma.talk_buddy.dto.AssessmentDto;
import kz.diploma.talk_buddy.dto.AssessmentResultDto;
import kz.diploma.talk_buddy.dto.AssessmentSubmissionDto;
import kz.diploma.talk_buddy.dto.TestSubmissionDto;
import kz.diploma.talk_buddy.entity.User;
import kz.diploma.talk_buddy.repository.UserRepository;
import kz.diploma.talk_buddy.service.AiAssessmentService;
import kz.diploma.talk_buddy.service.QuestionService;
import kz.diploma.talk_buddy.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final TopicService topicService;
    private final QuestionService questionService;
    private final UserRepository userRepository;
    private final AiAssessmentService aiService;


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

        // оценка
        AssessmentResultDto result = aiService.evaluate(submission);

        // сохраняем уровень
        user.setLevel(result.getLevel());
        userRepository.save(user);

        // очищаем тест
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
}