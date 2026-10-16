package com.taxicab.repository;

import com.taxicab.model.CustomerRideTracking;

import java.util.Optional;

public interface CustomerRideRepository {
    CustomerRideTracking save(CustomerRideTracking tracking);
    Optional<CustomerRideTracking> findByBookingId(Long bookingId);
}
