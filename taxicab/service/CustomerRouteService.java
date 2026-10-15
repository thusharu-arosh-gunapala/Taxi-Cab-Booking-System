package com.taxicab.service;

import com.taxicab.dto.location.LocationRequest;
import com.taxicab.dto.route.RouteRequest;
import com.taxicab.dto.route.RouteResponse;
import com.taxicab.exception.RouteCalculationException;
import com.taxicab.geo.CoordinateValidator;
import com.taxicab.integration.maps.RoutingClient;
import com.taxicab.model.Route;
import com.taxicab.repository.RouteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerRouteService {

    private final RoutingClient routingClient;
    private final RouteRepository routeRepository;
    private final DistanceService distanceService;
    private final EtaService etaService;

    public CustomerRouteService(RoutingClient routingClient, RouteRepository routeRepository,
                               DistanceService distanceService, EtaService etaService) {
        this.routingClient = routingClient;
        this.routeRepository = routeRepository;
        this.distanceService = distanceService;
        this.etaService = etaService;
    }

    public RouteResponse calculate(RouteRequest request) {
        if (request == null) {
            throw new RouteCalculationException("Route request is required.");
        }
        CoordinateValidator.validateLocationRequest(request.getPickup());
        CoordinateValidator.validateLocationRequest(request.getDestination());

        Route route = routingClient.route(request);
        if (route == null || route.getPickup() == null || route.getDestination() == null) {
            throw new RouteCalculationException("No valid road route could be calculated.");
        }

        distanceService.validateDistance(route.getRoadDistanceMetres());
        double durationSeconds = route.getDurationSeconds();
        if (durationSeconds < 0) {
            throw new RouteCalculationException("The route duration is invalid.");
        }

        Route savedRoute = routeRepository.save(route);
        LocationRequest pickup = new LocationRequest(savedRoute.getPickup().getLatitude(), savedRoute.getPickup().getLongitude(), savedRoute.getPickup().getAddress());
        LocationRequest destination = new LocationRequest(savedRoute.getDestination().getLatitude(), savedRoute.getDestination().getLongitude(), savedRoute.getDestination().getAddress());
        return new RouteResponse(
                savedRoute.getId(),
                pickup,
                destination,
                savedRoute.getRoadDistanceMetres(),
                savedRoute.getDurationSeconds(),
                savedRoute.getGeometry() == null ? List.of() : savedRoute.getGeometry(),
                savedRoute.getCalculatedAt()
        );
    }
}
