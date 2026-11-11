package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.PassengerRegistrationForm;
import com.serendib.taxibooking.service.PassengerService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final PassengerService passengerService;

    public AuthController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registration", new PassengerRegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registration") PassengerRegistrationForm form,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "The passwords do not match.");
        }
        if (bindingResult.hasErrors()) {
            form.setPassword("");
            form.setConfirmPassword("");
            return "auth/register";
        }

        try {
            passengerService.register(form);
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("email", "email.unavailable", exception.getMessage());
            form.setPassword("");
            form.setConfirmPassword("");
            return "auth/register";
        }
        redirectAttributes.addFlashAttribute("notice", "Your account is ready. Sign in to request a ride.");
        return "redirect:/login";
    }
}
