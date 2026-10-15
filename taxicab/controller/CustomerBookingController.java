package com.taxicab.controller;

import com.taxicab.dto.booking.BookingStatusResponse;
import com.taxicab.dto.booking.DriverAssignmentResponse;
import com.taxicab.service.CustomerBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
public class CustomerBookingController {

    private final CustomerBookingService customerBookingService;

    public CustomerBookingController(CustomerBookingService customerBookingService) {
        this.customerBookingService = customerBookingService;
    }

    @PostMapping("/bookings/{bookingId}/find-driver")
    public ResponseEntity<DriverAssignmentResponse> findDriver(@PathVariable Long bookingId) {
        return ResponseEntity.ok(customerBookingService.findDriver(bookingId));
    }

    @GetMapping("/bookings/{bookingId}/status")
    public ResponseEntity<BookingStatusResponse> getStatus(@PathVariable Long bookingId) {
        return ResponseEntity.ok(customerBookingService.getStatus(bookingId));
    }
}
