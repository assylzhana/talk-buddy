package kz.diploma.talk_buddy.controller;

import kz.diploma.talk_buddy.dto.TestSubmissionDto;
import kz.diploma.talk_buddy.service.QuestionService;
import kz.diploma.talk_buddy.service.TopicService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/tests")
    public String tests(Model model) {
        model.addAttribute("topics", topicService.findAll());
        return "student/tests";
    }

}