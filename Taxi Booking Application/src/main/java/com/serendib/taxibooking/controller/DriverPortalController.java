package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.DriverPasswordChangeForm;
import com.serendib.taxibooking.entity.Driver;
import com.serendib.taxibooking.repository.VehicleRepository;
import com.serendib.taxibooking.repository.BookingRepository;
import com.serendib.taxibooking.entity.BookingStatus;
import com.serendib.taxibooking.service.DriverService;
import com.serendib.taxibooking.service.BookingIntegrationService;
import com.serendib.taxibooking.service.BookingEventPublisher;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DriverPortalController {

    private final DriverService driverService;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final BookingIntegrationService bookingIntegrationService;
    private final BookingEventPublisher bookingEventPublisher;

    public DriverPortalController(DriverService driverService,
                                  VehicleRepository vehicleRepository,
                                  BookingRepository bookingRepository,
                                  BookingIntegrationService bookingIntegrationService,
                                  BookingEventPublisher bookingEventPublisher) {
        this.driverService = driverService;
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.bookingIntegrationService = bookingIntegrationService;
        this.bookingEventPublisher = bookingEventPublisher;
    }

    @GetMapping("/driver/login")
    public String login() {
        return "redirect:/login?driver";
    }

    @GetMapping("/driver/change-password")
    public String changePasswordForm(@AuthenticationPrincipal Driver driver, Model model) {
        if (!driver.isMustChangePassword()) {
            return "redirect:/driver/dashboard";
        }
        model.addAttribute("passwordForm", new DriverPasswordChangeForm());
        return "driver/change-password";
    }

    @PostMapping("/driver/change-password")
    public String changePassword(@AuthenticationPrincipal Driver driver,
                                 @Valid @ModelAttribute("passwordForm") DriverPasswordChangeForm form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "The passwords do not match.");
        }
        if (bindingResult.hasErrors()) {
            return "driver/change-password";
        }
        try {
            driverService.changeTemporaryPassword(driver, form.getPassword());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("passwordError", exception.getMessage());
            return "driver/change-password";
        }
        redirectAttributes.addFlashAttribute("notice", "Your password was changed successfully.");
        return "redirect:/driver/dashboard";
    }

    @GetMapping("/driver/dashboard")
    public String dashboard(@AuthenticationPrincipal Driver driver, Model model) {
        Driver currentDriver = driverService.findById(driver.getId());
        model.addAttribute("driverProfile", currentDriver);
        model.addAttribute("vehicles", vehicleRepository.findAllByDriverId(currentDriver.getId()));
        var bookings = bookingRepository.findAllByAssignedDriverReferenceOrderByCreatedAtDesc(
                currentDriver.getDriverReference());
        model.addAttribute("rideOffers", bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.DRIVER_ASSIGNED).toList());
        model.addAttribute("currentTrips", bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.ACCEPTED
                        || booking.getStatus() == BookingStatus.ARRIVING
                        || booking.getStatus() == BookingStatus.IN_PROGRESS).toList());
        model.addAttribute("completedTrips", bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.COMPLETED).toList());
        return "driver/dashboard";
    }

    @GetMapping("/driver/jobs")
    public String jobs(@AuthenticationPrincipal Driver driver, Model model) {
        model.addAttribute("jobs", bookingRepository.findAllByAssignedDriverReferenceOrderByCreatedAtDesc(
                driver.getDriverReference()).stream()
                .filter(booking -> booking.getStatus() == BookingStatus.DRIVER_ASSIGNED).toList());
        return "driver/jobs";
    }

    @PostMapping("/driver/jobs/{bookingId}/response")
    public String respondToJob(@PathVariable Long bookingId,
                               @AuthenticationPrincipal Driver driver,
                               @org.springframework.web.bind.annotation.RequestParam boolean accepted,
                               RedirectAttributes redirectAttributes) {
        try {
            var booking = bookingIntegrationService.respondToAssignment(
                    bookingId, driver.getDriverReference(), accepted);
            bookingEventPublisher.publish(booking);
            redirectAttributes.addFlashAttribute("notice",
                    accepted ? "Ride accepted." : "Ride offer declined.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/driver/jobs";
    }

    @GetMapping("/driver/trips")
    public String trips(@AuthenticationPrincipal Driver driver, Model model) {
        var trips = bookingRepository.findAllByAssignedDriverReferenceOrderByCreatedAtDesc(
                driver.getDriverReference());
        model.addAttribute("currentTrips", trips.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.ACCEPTED
                        || booking.getStatus() == BookingStatus.ARRIVING
                        || booking.getStatus() == BookingStatus.IN_PROGRESS).toList());
        model.addAttribute("completedTrips", trips.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.COMPLETED).toList());
        model.addAttribute("cancelledTrips", trips.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CANCELLED).toList());
        return "driver/trips";
    }

    @PostMapping("/driver/availability")
    public String toggleAvailability(@AuthenticationPrincipal Driver driver,
                                     RedirectAttributes redirectAttributes) {
        try {
            driverService.toggleOnline(driver.getId());
            redirectAttributes.addFlashAttribute("notice", "Your availability was updated.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/driver/dashboard";
    }
}
