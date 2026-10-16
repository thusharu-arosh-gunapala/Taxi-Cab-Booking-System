package com.taxicab.dto.route;

import com.taxicab.dto.location.LocationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class RouteRequest {

    @NotNull(message = "Pickup is required")
    @Valid
    private LocationRequest pickup;

    @NotNull(message = "Destination is required")
    @Valid
    private LocationRequest destination;

    public RouteRequest() {
    }

    public RouteRequest(LocationRequest pickup, LocationRequest destination) {
        this.pickup = pickup;
        this.destination = destination;
    }

    public LocationRequest getPickup() {
        return pickup;
    }

    public void setPickup(LocationRequest pickup) {
        this.pickup = pickup;
    }

    public LocationRequest getDestination() {
        return destination;
    }

    public void setDestination(LocationRequest destination) {
        this.destination = destination;
    }
}
