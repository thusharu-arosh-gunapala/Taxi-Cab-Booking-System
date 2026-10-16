package com.taxicab.service;

import com.taxicab.dto.tracking.CustomerTrackingResponse;
import com.taxicab.dto.tracking.DriverLocationResponse;
import com.taxicab.exception.TrackingUnavailableException;
import com.taxicab.model.Booking;
import com.taxicab.model.CustomerRideTracking;
import com.taxicab.repository.CustomerRideRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class CustomerTrackingService {

    private final CustomerBookingService customerBookingService;
    private final CustomerRideRepository customerRideRepository;

    public CustomerTrackingService(CustomerBookingService customerBookingService, CustomerRideRepository customerRideRepository) {
        this.customerBookingService = customerBookingService;
        this.customerRideRepository = customerRideRepository;
    }

    public DriverLocationResponse getDriverLocation(Long bookingId, Long customerId) {
        customerBookingService.validateBookingOwnership(bookingId, customerId);
        Booking booking = customerBookingService.getBooking(bookingId);
        Optional<CustomerRideTracking> ride = customerRideRepository.findByBookingId(bookingId);
        if (ride.isEmpty()) {
            return new DriverLocationResponse(bookingId, booking.getDriverId(), 6.9271, 79.8612, Instant.now(), booking.getStatus());
        }
        CustomerRideTracking tracking = ride.get();
        return new DriverLocationResponse(
                bookingId,
                tracking.getDriverId(),
                tracking.getDriverLatitude(),
                tracking.getDriverLongitude(),
                tracking.getLocationTimestamp(),
                booking.getStatus()
        );
    }

    public CustomerTrackingResponse getTracking(Long bookingId, Long customerId) {
        customerBookingService.validateBookingOwnership(bookingId, customerId);
        Booking booking = customerBookingService.getBooking(bookingId);
        Optional<CustomerRideTracking> ride = customerRideRepository.findByBookingId(bookingId);
        if (ride.isEmpty()) {
            throw new TrackingUnavailableException("Driver tracking details are not available for booking " + bookingId + ".");
        }

        CustomerRideTracking tracking = ride.get();
        return new CustomerTrackingResponse(
                bookingId,
                booking.getStatus(),
                tracking.getDriverId(),
                tracking.getDriverLatitude(),
                tracking.getDriverLongitude(),
                tracking.getLocationTimestamp(),
                tracking.getSpeedKph(),
                tracking.getDistanceToPickupKm(),
                tracking.getEtaToPickupMinutes(),
                tracking.getDistanceTravelledKm(),
                tracking.getDistanceRemainingKm(),
                tracking.getTripEtaMinutes(),
                tracking.getElapsedMinutes(),
                tracking.getAverageSpeedKph()
        );
    }
}
