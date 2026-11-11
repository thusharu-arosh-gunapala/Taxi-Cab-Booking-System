package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.BookingForm;
import com.serendib.taxibooking.dto.BookingCancellationForm;
import com.serendib.taxibooking.entity.Passenger;
import com.serendib.taxibooking.model.VehicleCategory;
import com.serendib.taxibooking.service.BookingService;
import com.serendib.taxibooking.service.BookingEventPublisher;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BookingController {

    private final BookingService bookingService;
    private final BookingEventPublisher bookingEventPublisher;

    public BookingController(BookingService bookingService, BookingEventPublisher bookingEventPublisher) {
        this.bookingService = bookingService;
        this.bookingEventPublisher = bookingEventPublisher;
    }

    @GetMapping("/bookings")
    public String bookings(@AuthenticationPrincipal Passenger passenger, Model model) {
        model.addAttribute("bookings", bookingService.findForPassenger(passenger));
        return "booking/list";
    }

    @GetMapping("/bookings/{id}")
    public String bookingDetails(@PathVariable Long id,
                                 @AuthenticationPrincipal Passenger passenger,
                                 Model model) {
        model.addAttribute("booking", bookingService.findOwnedBooking(id, passenger));
        model.addAttribute("cancellation", new BookingCancellationForm());
        return "booking/detail";
    }

    @GetMapping(value = "/api/bookings/{id}/events", produces = "text/event-stream")
    @ResponseBody
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter bookingEvents(
            @PathVariable Long id,
            @AuthenticationPrincipal Passenger passenger) {
        return bookingEventPublisher.subscribe(bookingService.findOwnedBooking(id, passenger));
    }

    @GetMapping("/bookings/new")
    public String newBooking(Model model) {
        model.addAttribute("booking", new BookingForm());
        model.addAttribute("bookingId", null);
        addVehicleCategories(model);
        return "booking/new";
    }

    @GetMapping("/bookings/{id}/edit")
    public String editBooking(@PathVariable Long id,
                              @AuthenticationPrincipal Passenger passenger,
                              Model model) {
        var existing = bookingService.findOwnedBooking(id, passenger);
        if (existing.getStatus() != com.serendib.taxibooking.entity.BookingStatus.PENDING) {
            model.addAttribute("error", "Only pending bookings can be changed.");
            model.addAttribute("booking", existing);
            model.addAttribute("cancellation", new BookingCancellationForm());
            return "booking/detail";
        }
        model.addAttribute("bookingId", id);
        model.addAttribute("booking", formFor(existing));
        addVehicleCategories(model);
        return "booking/new";
    }

    @PostMapping("/bookings")
    public String createBooking(@AuthenticationPrincipal Passenger passenger,
                                @Valid @ModelAttribute("booking") BookingForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addVehicleCategories(model);
            return "booking/new";
        }

        try {
            var booking = bookingService.create(passenger, form);
            redirectAttributes.addFlashAttribute(
                    "notice", "Booking " + booking.getBookingReference()
                            + " has been saved. A driver has not been assigned yet.");
            return "redirect:/bookings";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("booking.invalid", exception.getMessage());
            addVehicleCategories(model);
            return "booking/new";
        }
    }

    @PostMapping("/bookings/{id}")
    public String updateBooking(@PathVariable Long id,
                                @AuthenticationPrincipal Passenger passenger,
                                @Valid @ModelAttribute("booking") BookingForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("bookingId", id);
            addVehicleCategories(model);
            return "booking/new";
        }
        try {
            bookingService.update(id, passenger, form);
            redirectAttributes.addFlashAttribute("notice", "Your booking has been updated.");
            bookingEventPublisher.publish(bookingService.findOwnedBooking(id, passenger));
            return "redirect:/bookings/" + id;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("booking.invalid", exception.getMessage());
            model.addAttribute("bookingId", id);
            addVehicleCategories(model);
            return "booking/new";
        }
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable Long id,
                                @AuthenticationPrincipal Passenger passenger,
                                @Valid @ModelAttribute("cancellation") BookingCancellationForm cancellation,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please provide a cancellation reason.");
            return "redirect:/bookings/" + id;
        }
        try {
            bookingService.cancel(id, passenger, cancellation.getReason());
            bookingEventPublisher.publish(bookingService.findOwnedBooking(id, passenger));
            redirectAttributes.addFlashAttribute("notice", "The booking was cancelled.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/bookings/" + id;
    }

    private BookingForm formFor(com.serendib.taxibooking.entity.Booking existing) {
        BookingForm form = new BookingForm();
        form.setPickupAddress(existing.getPickupAddress());
        form.setDestinationAddress(existing.getDestinationAddress());
        form.setPickupLatitude(existing.getPickupLatitude());
        form.setPickupLongitude(existing.getPickupLongitude());
        form.setDestinationLatitude(existing.getDestinationLatitude());
        form.setDestinationLongitude(existing.getDestinationLongitude());
        form.setVehicleCategory(existing.getVehicleCategory());
        form.setPeakHour(existing.isPeakHour());
        form.setBookingTime(existing.getBookingTime());
        return form;
    }

    private void addVehicleCategories(Model model) {
        model.addAttribute("vehicleCategories", VehicleCategory.values());
    }
}
