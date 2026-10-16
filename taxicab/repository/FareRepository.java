package com.taxicab.repository;

import com.taxicab.model.Fare;

import java.util.Optional;

public interface FareRepository {
    Fare save(Fare fare);
    Optional<Fare> findByRouteId(Long routeId);
}
