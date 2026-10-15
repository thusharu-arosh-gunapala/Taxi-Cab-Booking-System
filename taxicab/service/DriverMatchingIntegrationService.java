package com.taxicab.service;

import com.taxicab.dto.booking.DriverAssignmentResponse;

public interface DriverMatchingIntegrationService {
    DriverAssignmentResponse requestDriver(Long bookingId);
    DriverAssignmentResponse getDriverAssignment(Long bookingId);
}
