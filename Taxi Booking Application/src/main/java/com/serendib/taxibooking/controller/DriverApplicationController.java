package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.DriverRegistrationForm;
import com.serendib.taxibooking.dto.DriverAccountRegistrationForm;
import com.serendib.taxibooking.entity.Driver;
import com.serendib.taxibooking.entity.DriverApplicationStatus;
import com.serendib.taxibooking.model.VehicleCategory;
import com.serendib.taxibooking.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DriverApplicationController {

    private final DriverService driverService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public DriverApplicationController(DriverService driverService,
                                       AuthenticationManager authenticationManager,
                                       SecurityContextRepository securityContextRepository) {
        this.driverService = driverService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/driver/register")
    public String registrationForm(Model model) {
        model.addAttribute("registration", new DriverAccountRegistrationForm());
        return "driver/register";
    }

    @PostMapping("/driver/register")
    public String register(@Valid @ModelAttribute("registration") DriverAccountRegistrationForm form,
                           BindingResult bindingResult,
                           Model model,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "The passwords do not match.");
        }
        if (bindingResult.hasErrors()) {
            form.setPassword("");
            form.setConfirmPassword("");
            return "driver/register";
        }
        try {
            driverService.register(form);
            var authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(form.getEmail().trim(), form.getPassword()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            return "redirect:/driver/application";
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("email", "email.unavailable", exception.getMessage());
            form.setPassword("");
            form.setConfirmPassword("");
            return "driver/register";
        }
    }

    @GetMapping("/driver/apply")
    public String applicationFormRedirect() {
        return "redirect:/driver/application";
    }

    @GetMapping("/driver/application")
    public String applicationForm(@AuthenticationPrincipal Driver driver, Model model) {
        if (driver.getApplicationStatus() == DriverApplicationStatus.PENDING) {
            return "redirect:/driver/status";
        }
        if (driver.getApplicationStatus() == DriverApplicationStatus.APPROVED) {
            return "redirect:/driver/dashboard";
        }
        if (!model.containsAttribute("driverApplication")) {
            DriverRegistrationForm form = new DriverRegistrationForm();
            form.setName(driver.getName());
            form.setPhone(driver.getPhone());
            model.addAttribute("driverApplication", form);
        }
        model.addAttribute("driverEmail", driver.getEmail());
        model.addAttribute("vehicleCategories", VehicleCategory.values());
        return "driver/application";
    }

    @PostMapping("/driver/application")
    public String apply(@AuthenticationPrincipal Driver driver,
                        @Valid @ModelAttribute("driverApplication") DriverRegistrationForm form,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("driverEmail", driver.getEmail());
            model.addAttribute("vehicleCategories", VehicleCategory.values());
            return "driver/application";
        }
        try {
            driverService.applyWithVehicle(driver, form);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("driver.application.invalid", exception.getMessage());
            model.addAttribute("driverEmail", driver.getEmail());
            model.addAttribute("vehicleCategories", VehicleCategory.values());
            return "driver/application";
        }
        return "redirect:/driver/application/submitted";
    }

    @GetMapping("/driver/application/submitted")
    public String applicationSubmitted(@AuthenticationPrincipal Driver driver) {
        return driverService.findById(driver.getId()).getApplicationStatus() == DriverApplicationStatus.PENDING
                ? "driver/application-submitted" : "redirect:/driver/application";
    }

    @GetMapping("/driver/status")
    public String applicationStatus(@AuthenticationPrincipal Driver driver, Model model) {
        model.addAttribute("driverProfile", driverService.findById(driver.getId()));
        return "driver/status";
    }
}
