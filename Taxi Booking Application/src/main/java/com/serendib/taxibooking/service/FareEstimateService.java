package com.serendib.taxibooking.service;

import com.serendib.taxibooking.config.FareConfig;
import com.serendib.taxibooking.dto.FareEstimateRequest;
import com.serendib.taxibooking.dto.FareEstimateResponse;
import com.serendib.taxibooking.model.FareRule;
import com.serendib.taxibooking.model.RouteEstimate;
import com.serendib.taxibooking.model.VehicleCategory;
import com.serendib.taxibooking.strategy.FareCalculationStrategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FareEstimateService {

    private final FareConfig fareConfig;
    private final FareCalculationStrategy fareCalculationStrategy;
    private final RoutingProvider routingProvider;

    public FareEstimateService(FareConfig fareConfig, FareCalculationStrategy fareCalculationStrategy,
                               RoutingProvider routingProvider) {
        this.fareConfig = fareConfig;
        this.fareCalculationStrategy = fareCalculationStrategy;
        this.routingProvider = routingProvider;
    }

    public FareEstimateResponse estimate(FareEstimateRequest request) {
        validateCoordinates(request);
        RouteEstimate route = routingProvider.route(
                request.pickupLongitude(),
                request.pickupLatitude(),
                request.destinationLongitude(),
                request.destinationLatitude());
        double kilometres = route.distanceMetres() / 1000.0;
        if (!Double.isFinite(kilometres) || kilometres <= 0) {
            throw new IllegalArgumentException("The routing service did not provide a valid road distance.");
        }
        if (!Double.isFinite(route.durationSeconds()) || route.durationSeconds() <= 0) {
            throw new IllegalArgumentException("The routing service did not provide a valid travel duration.");
        }

        VehicleCategory category = request.vehicleCategory();
        FareRule fareRule = fareConfig.getRule(category);
        BigDecimal distance = BigDecimal.valueOf(kilometres).setScale(3, RoundingMode.HALF_UP);
        BigDecimal fare = BigDecimal.valueOf(
                fareCalculationStrategy.calculate(kilometres, fareRule, request.peakHour()))
                .setScale(2, RoundingMode.HALF_UP);
        return new FareEstimateResponse(
                category,
                distance,
                Math.round(route.durationSeconds() / 60.0),
                fare,
                fareConfig.getCurrency(),
                "Road route estimate.",
                route.coordinates());
    }

    private void validateCoordinates(FareEstimateRequest request) {
        if (request == null || request.pickupLatitude() == null || request.pickupLongitude() == null
                || request.destinationLatitude() == null || request.destinationLongitude() == null
                || request.vehicleCategory() == null) {
            throw new IllegalArgumentException("Coordinates and vehicle category are required for an estimate.");
        }
        validateLatitude(request.pickupLatitude());
        validateLatitude(request.destinationLatitude());
        validateLongitude(request.pickupLongitude());
        validateLongitude(request.destinationLongitude());
    }

    private void validateLatitude(double latitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees.");
        }
    }

    private void validateLongitude(double longitude) {
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees.");
        }
    }
}
