package com.serendib.taxibooking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import com.serendib.taxibooking.entity.Driver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@RequestParam(required = false) String logout,
                       Authentication authentication,
                       Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return "redirect:/admin";
        }
        if (logout != null) {
            model.addAttribute("notice", "You have been signed out.");
        }
        return "index";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            boolean admin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            if (admin) {
                return "redirect:/admin";
            }
            if (authentication.getPrincipal() instanceof Driver driver) {
                return switch (driver.getApplicationStatus()) {
                    case NOT_SUBMITTED, REJECTED, RESUBMISSION_REQUIRED -> "redirect:/driver/application";
                    case PENDING -> "redirect:/driver/status";
                    case APPROVED -> driver.isSuspended()
                            ? "redirect:/driver/status?suspended" : "redirect:/driver/dashboard";
                };
            }
            return "redirect:/bookings";
        }
        return "auth/login";
    }
}
