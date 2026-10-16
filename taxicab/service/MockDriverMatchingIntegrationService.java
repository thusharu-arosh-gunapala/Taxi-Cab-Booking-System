package com.taxicab.service;

import com.taxicab.dto.booking.DriverAssignmentResponse;
import com.taxicab.exception.BookingNotFoundException;
import com.taxicab.integration.driver.DriverAvailabilityClient;
import org.springframework.stereotype.Service;

@Service
public class MockDriverMatchingIntegrationService implements DriverMatchingIntegrationService {

    private final DriverAvailabilityClient driverAvailabilityClient;

    public MockDriverMatchingIntegrationService(DriverAvailabilityClient driverAvailabilityClient) {
        this.driverAvailabilityClient = driverAvailabilityClient;
    }

    @Override
    public DriverAssignmentResponse requestDriver(Long bookingId) {
        if (bookingId == null) {
            throw new BookingNotFoundException(null);
        }
        return driverAvailabilityClient.requestDriver(bookingId);
    }

    @Override
    public DriverAssignmentResponse getDriverAssignment(Long bookingId) {
        if (bookingId == null) {
            throw new BookingNotFoundException(null);
        }
        return driverAvailabilityClient.getAssignment(bookingId);
    }
}
