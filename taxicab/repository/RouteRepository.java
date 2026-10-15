package com.taxicab.repository;

import com.taxicab.model.Route;

import java.util.Optional;

public interface RouteRepository {
    Route save(Route route);
    Optional<Route> findById(Long id);
}
