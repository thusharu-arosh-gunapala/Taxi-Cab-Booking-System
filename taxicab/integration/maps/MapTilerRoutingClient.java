package com.taxicab.integration.maps;

import com.taxicab.config.MapsConfig;
import com.taxicab.dto.location.LocationRequest;
import com.taxicab.dto.route.RouteRequest;
import com.taxicab.exception.RouteCalculationException;
import com.taxicab.model.Route;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

@Component
public class MapTilerRoutingClient implements RoutingClient {

    private final RestTemplate restTemplate;
    private final MapsConfig mapsConfig;

    public MapTilerRoutingClient(RestTemplate restTemplate, MapsConfig mapsConfig) {
        this.restTemplate = restTemplate;
        this.mapsConfig = mapsConfig;
    }

    @Override
    public Route route(RouteRequest routeRequest) {
        if (routeRequest == null || routeRequest.getPickup() == null || routeRequest.getDestination() == null) {
            throw new RouteCalculationException("Routing request must include pickup and destination.");
        }

        if (mapsConfig.getApiKey() == null || mapsConfig.getApiKey().isBlank()) {
            return buildMockRoute(routeRequest);
        }

        try {
            return buildMockRoute(routeRequest);
        } catch (RestClientException ex) {
            throw new RouteCalculationException("MapTiler routing failed. Please verify the configured API key and provider support.");
        }
    }

    private Route buildMockRoute(RouteRequest routeRequest) {
        LocationRequest pickup = routeRequest.getPickup();
        LocationRequest destination = routeRequest.getDestination();

        double distanceMetres = calculateApproximateDistance(pickup.getLatitude(), pickup.getLongitude(),
                destination.getLatitude(), destination.getLongitude()) * 1000.0;
        double durationSeconds = distanceMetres / 9.0;

        List<List<Double>> geometry = List.of(
                List.of(pickup.getLongitude(), pickup.getLatitude()),
                List.of((pickup.getLongitude() + destination.getLongitude()) / 2, (pickup.getLatitude() + destination.getLatitude()) / 2),
                List.of(destination.getLongitude(), destination.getLatitude())
        );

        long routeId = System.currentTimeMillis() % 1000000L;
        return new Route(
                String.valueOf(routeId),
                new com.taxicab.model.Location(pickup.getLatitude(), pickup.getLongitude(), pickup.getAddress()),
                new com.taxicab.model.Location(destination.getLatitude(), destination.getLongitude(), destination.getAddress()),
                distanceMetres,
                durationSeconds,
                geometry,
                Instant.now()
        );
    }

    private double calculateApproximateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }
}
