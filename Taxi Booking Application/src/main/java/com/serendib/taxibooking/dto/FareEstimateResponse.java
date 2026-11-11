package com.serendib.taxibooking.dto;

import com.serendib.taxibooking.model.VehicleCategory;

import java.math.BigDecimal;
import java.util.List;

public record FareEstimateResponse(
        VehicleCategory vehicleCategory,
        BigDecimal distanceKilometres,
        long durationMinutes,
        BigDecimal estimatedFare,
        String currency,
        String distanceBasis,
        List<List<Double>> routeCoordinates) {
}
