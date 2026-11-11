package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.FirstAdministratorForm;
import com.serendib.taxibooking.repository.AdministratorRepository;
import com.serendib.taxibooking.service.AdminBootstrapService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Controller
public class AdminAccessController {

    private final AdministratorRepository administratorRepository;
    private final AdminBootstrapService adminBootstrapService;

    public AdminAccessController(AdministratorRepository administratorRepository,
                                 AdminBootstrapService adminBootstrapService) {
        this.administratorRepository = administratorRepository;
        this.adminBootstrapService = adminBootstrapService;
    }

    @GetMapping("/admin/login")
    public String redirectToSharedSignIn() {
        return "redirect:/login";
    }

    @GetMapping("/admin/setup")
    public String setupPage(HttpServletRequest request, Model model) {
        requireLocalRequest(request);
        if (administratorRepository.count() > 0) {
            return "redirect:/login";
        }
        model.addAttribute("firstAdministratorForm", new FirstAdministratorForm());
        return "auth/admin-setup";
    }

    @PostMapping("/admin/setup")
    public String createFirstAdministrator(HttpServletRequest request,
                                           @Valid @ModelAttribute("firstAdministratorForm")
                                           FirstAdministratorForm form,
                                           BindingResult bindingResult,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {
        requireLocalRequest(request);
        if (administratorRepository.count() > 0) {
            return "redirect:/login";
        }
        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "The passwords do not match.");
        }
        if (bindingResult.hasErrors()) {
            return "auth/admin-setup";
        }
        try {
            adminBootstrapService.createFirstAdministrator(form.getName(), form.getEmail(), form.getPassword());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("administrator.setup.invalid", exception.getMessage());
            return "auth/admin-setup";
        }
        redirectAttributes.addFlashAttribute("notice", "Administrator account created. Sign in with your new credentials.");
        return "redirect:/login";
    }

    private void requireLocalRequest(HttpServletRequest request) {
        try {
            if (!InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } catch (UnknownHostException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
