package com.taxicab.dto.fare;

import com.taxicab.model.VehicleCategory;
import jakarta.validation.constraints.NotNull;

public class FareRequest {

    @NotNull(message = "Route ID is required")
    private Long routeId;

    @NotNull(message = "Vehicle category is required")
    private VehicleCategory vehicleCategory;

    private boolean peakHour = false;

    public FareRequest() {
    }

    public FareRequest(Long routeId, VehicleCategory vehicleCategory, boolean peakHour) {
        this.routeId = routeId;
        this.vehicleCategory = vehicleCategory;
        this.peakHour = peakHour;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public VehicleCategory getVehicleCategory() {
        return vehicleCategory;
    }

    public void setVehicleCategory(VehicleCategory vehicleCategory) {
        this.vehicleCategory = vehicleCategory;
    }

    public boolean isPeakHour() {
        return peakHour;
    }

    public void setPeakHour(boolean peakHour) {
        this.peakHour = peakHour;
    }
}
