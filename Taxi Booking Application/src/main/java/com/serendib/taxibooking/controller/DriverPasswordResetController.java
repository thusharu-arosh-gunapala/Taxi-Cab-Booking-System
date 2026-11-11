package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.DriverPasswordResetForm;
import com.serendib.taxibooking.dto.DriverPasswordResetRequestForm;
import com.serendib.taxibooking.service.DriverPasswordResetService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DriverPasswordResetController {

    private final DriverPasswordResetService resetService;

    public DriverPasswordResetController(DriverPasswordResetService resetService) {
        this.resetService = resetService;
    }

    @GetMapping("/driver/forgot-password")
    public String forgotPasswordForm(Model model) {
        model.addAttribute("requestForm", new DriverPasswordResetRequestForm());
        return "driver/forgot-password";
    }

    @PostMapping("/driver/forgot-password")
    public String requestPasswordReset(@Valid @ModelAttribute("requestForm") DriverPasswordResetRequestForm form,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "driver/forgot-password";
        }
        resetService.requestReset(form.getEmail());
        redirectAttributes.addFlashAttribute("notice",
                "If the address belongs to an approved driver account, a reset link will be sent.");
        return "redirect:/driver/forgot-password";
    }

    @GetMapping("/driver/reset-password")
    public String resetPasswordForm(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token == null ? "" : token);
        model.addAttribute("validToken", resetService.isTokenValid(token));
        model.addAttribute("passwordForm", new DriverPasswordResetForm());
        return "driver/reset-password";
    }

    @PostMapping("/driver/reset-password")
    public String resetPassword(@RequestParam String token,
                                @Valid @ModelAttribute("passwordForm") DriverPasswordResetForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "The passwords do not match.");
        }
        boolean validToken = resetService.isTokenValid(token);
        if (bindingResult.hasErrors() || !validToken) {
            model.addAttribute("token", token);
            model.addAttribute("validToken", validToken);
            if (!validToken) {
                model.addAttribute("resetError", "This password reset link is invalid or expired.");
            }
            return "driver/reset-password";
        }
        try {
            resetService.resetPassword(token, form.getPassword());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("token", token);
            model.addAttribute("validToken", false);
            model.addAttribute("resetError", exception.getMessage());
            return "driver/reset-password";
        }
        redirectAttributes.addFlashAttribute("notice", "Your password was reset. Sign in with your new password.");
        return "redirect:/login?driver";
    }
}
