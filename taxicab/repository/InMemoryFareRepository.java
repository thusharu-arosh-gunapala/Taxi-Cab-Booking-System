package com.taxicab.repository;

import com.taxicab.model.Fare;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryFareRepository implements FareRepository {
    private final Map<Long, Fare> fares = new ConcurrentHashMap<>();

    @Override
    public Fare save(Fare fare) {
        if (fare.getRouteId() == null) {
            throw new IllegalArgumentException("Fare route ID is required");
        }
        fares.put(fare.getRouteId(), fare);
        return fare;
    }

    @Override
    public Optional<Fare> findByRouteId(Long routeId) {
        return Optional.ofNullable(fares.get(routeId));
    }
}
