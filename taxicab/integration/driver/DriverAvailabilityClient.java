package com.taxicab.integration.driver;

import com.taxicab.dto.booking.DriverAssignmentResponse;

public interface DriverAvailabilityClient {
    DriverAssignmentResponse requestDriver(Long bookingId);
    DriverAssignmentResponse getAssignment(Long bookingId);
}
