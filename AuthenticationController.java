package com.taxibooking.controller; //AuthenticationController

import com.taxibooking.model.User;
import com.taxibooking.service.AuthenticationService;
import com.taxibooking.service.UserService;
import com.taxibooking.service.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthenticationController {

    private final AuthenticationService authService;
    private final UserService userService;

    public AuthenticationController(AuthenticationService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    // ---------- Register ----------
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new HashMap<String, String>());
        return "passenger/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam Map<String, String> params, Model model, RedirectAttributes ra) {
        try {
            userService.register(params);
            ra.addFlashAttribute("success", "Account created. Please log in.");
            return "redirect:/login";
        } catch (ValidationException e) {
            Map<String, String> form = new HashMap<>(params);
            form.remove("password");
            form.remove("confirmPassword");
            model.addAttribute("form", form);
            model.addAttribute("errors", e.getErrors());
            return "passenger/register";
        }
    }

    // ---------- Login / Logout ----------
    @GetMapping("/login")
    public String loginForm(HttpSession session) {
        return session.getAttribute("userId") != null ? "redirect:/passenger/dashboard" : "passenger/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        HttpServletRequest request, Model model) {
        try {
            User user = authService.login(email, password);
            HttpSession session = request.getSession(true);
            request.changeSessionId();                  // avoid session fixation
            session.setAttribute("userId", user.getUserId());
            return "redirect:/passenger/dashboard";
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("email", email);
            return "passenger/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        session.invalidate();
        ra.addFlashAttribute("success", "You have been logged out.");
        return "redirect:/login";
    }
}

