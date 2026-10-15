package com.taxicab.integration.driver;

import com.taxicab.dto.booking.DriverAssignmentResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockDriverAvailabilityClient implements DriverAvailabilityClient {

    private final Map<Long, DriverAssignmentResponse> assignments = new ConcurrentHashMap<>();

    @Override
    public DriverAssignmentResponse requestDriver(Long bookingId) {
        if (bookingId == null) {
            return new DriverAssignmentResponse(null, null, "INVALID_BOOKING", false, "Booking ID is required");
        }
        DriverAssignmentResponse existing = assignments.get(bookingId);
        if (existing != null) {
            return existing;
        }

        Long driverId = 200L + (bookingId % 10L);
        DriverAssignmentResponse response = new DriverAssignmentResponse(bookingId, driverId, "DRIVER_ASSIGNED", true,
                "Temporary mock assignment generated for development only.");
        assignments.put(bookingId, response);
        return response;
    }

    @Override
    public DriverAssignmentResponse getAssignment(Long bookingId) {
        return assignments.getOrDefault(bookingId, new DriverAssignmentResponse(bookingId, null, "NO_DRIVER_AVAILABLE", false,
                "No driver is currently available in the mock system."));
    }
}
