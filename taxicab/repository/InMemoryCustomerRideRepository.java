package com.taxicab.repository;

import com.taxicab.model.CustomerRideTracking;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCustomerRideRepository implements CustomerRideRepository {
    private final Map<Long, CustomerRideTracking> rides = new ConcurrentHashMap<>();

    @Override
    public CustomerRideTracking save(CustomerRideTracking tracking) {
        if (tracking.getBookingId() == null) {
            throw new IllegalArgumentException("Booking ID is required for tracking data");
        }
        rides.put(tracking.getBookingId(), tracking);
        return tracking;
    }

    @Override
    public Optional<CustomerRideTracking> findByBookingId(Long bookingId) {
        return Optional.ofNullable(rides.get(bookingId));
    }
}
