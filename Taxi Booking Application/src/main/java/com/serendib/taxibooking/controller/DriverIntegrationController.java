package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.DriverAssignmentRequest;
import com.serendib.taxibooking.dto.DriverResponseRequest;
import com.serendib.taxibooking.dto.BookingStatusEvent;
import com.serendib.taxibooking.entity.Booking;
import com.serendib.taxibooking.service.BookingIntegrationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/integrations/bookings")
public class DriverIntegrationController {

    private final BookingIntegrationService bookingIntegrationService;
    private final String integrationToken;

    public DriverIntegrationController(
            BookingIntegrationService bookingIntegrationService,
            @Value("${app.integrations.driver-token:}") String integrationToken) {
        this.bookingIntegrationService = bookingIntegrationService;
        this.integrationToken = integrationToken;
    }

    @PostMapping("/{id}/searching")
    public BookingStatusEvent beginSearch(@PathVariable Long id,
                                         @RequestHeader(name = "X-Integration-Token", required = false) String token) {
        verifyToken(token);
        return eventFor(bookingIntegrationService.beginSearch(id));
    }

    @PutMapping("/{id}/assignment")
    public BookingStatusEvent assignDriver(@PathVariable Long id,
                                           @RequestHeader(name = "X-Integration-Token", required = false) String token,
                                           @Valid @RequestBody DriverAssignmentRequest request) {
        verifyToken(token);
        return eventFor(bookingIntegrationService.assignDriver(id, request.driverReference()));
    }

    @PostMapping("/{id}/driver-response")
    public BookingStatusEvent respondToAssignment(@PathVariable Long id,
                                                  @RequestHeader(name = "X-Integration-Token", required = false) String token,
                                                  @RequestBody DriverResponseRequest request) {
        verifyToken(token);
        return eventFor(bookingIntegrationService.respondToAssignment(id, request.accepted()));
    }

    @PostMapping("/{id}/unavailable")
    public BookingStatusEvent noDriverAvailable(@PathVariable Long id,
                                                @RequestHeader(name = "X-Integration-Token", required = false) String token) {
        verifyToken(token);
        return eventFor(bookingIntegrationService.markNoDriverAvailable(id));
    }

    @PostMapping("/{id}/arriving")
    public BookingStatusEvent driverArriving(@PathVariable Long id,
                                             @RequestHeader(name = "X-Integration-Token", required = false) String token) {
        verifyToken(token);
        return eventFor(bookingIntegrationService.markDriverArriving(id));
    }

    @PostMapping("/{id}/start")
    public BookingStatusEvent startTrip(@PathVariable Long id,
                                        @RequestHeader(name = "X-Integration-Token", required = false) String token) {
        verifyToken(token);
        return eventFor(bookingIntegrationService.startTrip(id));
    }

    @PostMapping("/{id}/complete")
    public BookingStatusEvent completeTrip(@PathVariable Long id,
                                           @RequestHeader(name = "X-Integration-Token", required = false) String token) {
        verifyToken(token);
        return eventFor(bookingIntegrationService.completeTrip(id));
    }

    private BookingStatusEvent eventFor(Booking booking) {
        return new BookingStatusEvent(booking.getBookingReference(), booking.getStatus(), booking.getUpdatedAt());
    }

    private void verifyToken(String token) {
        if (integrationToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Driver integration is not configured.");
        }
        if (token == null || !MessageDigest.isEqual(
                integrationToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid integration credentials.");
        }
    }
}
