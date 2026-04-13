package kz.diploma.talk_buddy.controller;

import kz.diploma.talk_buddy.entity.Role;
import kz.diploma.talk_buddy.entity.User;
import kz.diploma.talk_buddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fistName,
                           @RequestParam String lastName,
                           @RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String email,
                           Model model) {

        if (userRepository.findByUsername(username).isPresent()) {
            model.addAttribute("error", "User already exists");
            return "register";
        }

        User user = new User();
        user.setFirstName(fistName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setLevel(null);
        user.setRole(Role.ROLE_STUDENT);

        userRepository.save(user);

        model.addAttribute("success", "Registration successful");
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/main")
    public String mainPage() {
        return "main";
    }
}