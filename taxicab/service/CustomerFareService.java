package com.taxicab.service;

import com.taxicab.config.FareConfig;
import com.taxicab.dto.fare.FareRequest;
import com.taxicab.dto.fare.FareResponse;
import com.taxicab.exception.FareCalculationException;
import com.taxicab.model.Fare;
import com.taxicab.model.FareRule;
import com.taxicab.model.Route;
import com.taxicab.model.VehicleCategory;
import com.taxicab.repository.FareRepository;
import com.taxicab.repository.RouteRepository;
import com.taxicab.strategy.FareCalculationStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CustomerFareService {

    private final FareRepository fareRepository;
    private final RouteRepository routeRepository;
    private final FareConfig fareConfig;
    private final FareCalculationStrategy fareCalculationStrategy;

    public CustomerFareService(FareRepository fareRepository, RouteRepository routeRepository,
                              FareConfig fareConfig, @Qualifier("vehicleFareStrategy") FareCalculationStrategy fareCalculationStrategy) {
        this.fareRepository = fareRepository;
        this.routeRepository = routeRepository;
        this.fareConfig = fareConfig;
        this.fareCalculationStrategy = fareCalculationStrategy;
    }

    public FareResponse calculateFare(FareRequest request) {
        if (request == null) {
            throw new FareCalculationException("Fare request is required.");
        }
        if (request.getRouteId() == null) {
            throw new FareCalculationException("Route ID is required to calculate fare.");
        }
        if (request.getVehicleCategory() == null) {
            throw new FareCalculationException("Vehicle category is required.");
        }

        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new FareCalculationException("Route not found for fare calculation."));

        FareRule rule = fareConfig.getRule(request.getVehicleCategory());
        double distanceKilometres = route.getRoadDistanceKilometres();
        if (distanceKilometres <= 0) {
            throw new FareCalculationException("Route distance must be positive to estimate a fare.");
        }

        double distanceCharge = distanceKilometres * rule.getPerKilometreRate();
        double applicableCharges = rule.getServiceCharge();
        if (request.isPeakHour()) {
            double multiplier = rule.getPeakHourMultiplier();
            distanceCharge *= multiplier;
            applicableCharges += (rule.getBaseFare() * (multiplier - 1.0));
        }

        double estimate = fareCalculationStrategy.calculate(distanceKilometres, rule, request.isPeakHour());
        double rounded = Math.round(estimate * 100.0) / 100.0;
        Fare fare = new Fare(route.getId() == null ? request.getRouteId() : Long.parseLong(route.getId()), request.getVehicleCategory(),
                rule.getBaseFare(), distanceCharge, applicableCharges, rounded, fareConfig.getCurrency(), Instant.now());
        fareRepository.save(fare);

        return new FareResponse(
                request.getVehicleCategory(),
                rule.getBaseFare(),
                distanceCharge,
                applicableCharges,
                rounded,
                fareConfig.getCurrency(),
                fare.getCalculatedAt()
        );
    }

    public FareResponse calculateFareForVehicle(Long routeId, VehicleCategory vehicleCategory, boolean peakHour) {
        return calculateFare(new FareRequest(routeId, vehicleCategory, peakHour));
    }
}
