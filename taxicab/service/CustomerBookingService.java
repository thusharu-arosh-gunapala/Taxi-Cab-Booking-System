package com.taxicab.service;

import com.taxicab.dto.booking.BookingStatusResponse;
import com.taxicab.dto.booking.DriverAssignmentResponse;
import com.taxicab.exception.BookingNotFoundException;
import com.taxicab.exception.UnauthorizedBookingAccessException;
import com.taxicab.integration.driver.DriverAvailabilityClient;
import com.taxicab.integration.driver.MockDriverAvailabilityClient;
import com.taxicab.model.Booking;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CustomerBookingService {

    private final Map<Long, Booking> bookings = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);
    private final DriverMatchingIntegrationService driverMatchingIntegrationService;

    public CustomerBookingService(DriverMatchingIntegrationService driverMatchingIntegrationService) {
        this.driverMatchingIntegrationService = driverMatchingIntegrationService != null
                ? driverMatchingIntegrationService
                : new DriverMatchingIntegrationService() {
                    private final DriverAvailabilityClient client = new MockDriverAvailabilityClient();

                    @Override
                    public DriverAssignmentResponse requestDriver(Long bookingId) {
                        return client.requestDriver(bookingId);
                    }

                    @Override
                    public DriverAssignmentResponse getDriverAssignment(Long bookingId) {
                        return client.getAssignment(bookingId);
                    }
                };
        seedSampleBookings();
    }

    public Booking createBooking(Long customerId, Long routeId, String vehicleType) {
        Booking booking = new Booking(sequence.getAndIncrement(), customerId, null, "REQUESTED", vehicleType, routeId);
        bookings.put(booking.getId(), booking);
        return booking;
    }

    public Booking getBooking(Long bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            throwBookingMissing(bookingId);
        }
        return booking;
    }

    public BookingStatusResponse getStatus(Long bookingId) {
        Booking booking = getBooking(bookingId);
        return new BookingStatusResponse(booking.getId(), booking.getStatus(), "Booking status retrieved successfully.", booking.getDriverId());
    }

    public DriverAssignmentResponse findDriver(Long bookingId) {
        Booking booking = getBooking(bookingId);
        DriverAssignmentResponse assignment = driverMatchingIntegrationService.requestDriver(booking.getId());
        booking.setDriverId(assignment.getDriverId());
        booking.setStatus(assignment.isAssigned() ? "DRIVER_ASSIGNED" : "NO_DRIVER_AVAILABLE");
        booking.setUpdatedAt(java.time.Instant.now());
        return assignment;
    }

    public boolean isBookingOwner(Long bookingId, Long customerId) {
        Booking booking = getBooking(bookingId);
        return booking.getCustomerId().equals(customerId);
    }

    public void validateBookingOwnership(Long bookingId, Long customerId) {
        if (!isBookingOwner(bookingId, customerId)) {
            throw new UnauthorizedBookingAccessException(bookingId);
        }
    }

    private Booking throwBookingMissing(Long bookingId) {
        throw new BookingNotFoundException(bookingId);
    }

    private void seedSampleBookings() {
        Booking sample = new Booking(1L, 1001L, 2001L, "DRIVER_ASSIGNED", "CAR", 101L);
        bookings.put(sample.getId(), sample);
    }
}
