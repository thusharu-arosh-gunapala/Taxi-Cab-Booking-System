package com.serendib.taxibooking.dto;

import com.serendib.taxibooking.model.VehicleCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record FareEstimateRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double pickupLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double pickupLongitude,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double destinationLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double destinationLongitude,
        @NotNull VehicleCategory vehicleCategory,
        boolean peakHour) {
}
