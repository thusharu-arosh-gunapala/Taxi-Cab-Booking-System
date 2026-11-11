package com.serendib.taxibooking.dto;

import com.serendib.taxibooking.model.VehicleCategory;

public record NearbyDriverResponse(
        VehicleCategory vehicleCategory,
        double latitude,
        double longitude,
        long distanceMetres) {
}
