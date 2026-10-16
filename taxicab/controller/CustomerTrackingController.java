package com.taxicab.controller;

import com.taxicab.dto.tracking.CustomerTrackingResponse;
import com.taxicab.dto.tracking.DriverLocationResponse;
import com.taxicab.service.CustomerTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
public class CustomerTrackingController {

    private final CustomerTrackingService customerTrackingService;

    public CustomerTrackingController(CustomerTrackingService customerTrackingService) {
        this.customerTrackingService = customerTrackingService;
    }

    @GetMapping("/bookings/{bookingId}/driver-location")
    public ResponseEntity<DriverLocationResponse> getDriverLocation(@PathVariable Long bookingId,
                                                                  @RequestParam Long customerId) {
        return ResponseEntity.ok(customerTrackingService.getDriverLocation(bookingId, customerId));
    }

    @GetMapping("/bookings/{bookingId}/tracking")
    public ResponseEntity<CustomerTrackingResponse> getTracking(@PathVariable Long bookingId,
                                                              @RequestParam Long customerId) {
        return ResponseEntity.ok(customerTrackingService.getTracking(bookingId, customerId));
    }
}
