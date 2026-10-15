package com.taxicab.repository;

import com.taxicab.model.Route;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryRouteRepository implements RouteRepository {
    private final Map<Long, Route> routes = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    @Override
    public Route save(Route route) {
        Long id = route.getId() == null ? sequence.getAndIncrement() : Long.parseLong(route.getId());
        route.setId(String.valueOf(id));
        routes.put(id, route);
        return route;
    }

    @Override
    public Optional<Route> findById(Long id) {
        return Optional.ofNullable(routes.get(id));
    }
}
