package com.serendib.taxibooking.model;

import java.util.List;

public record RouteEstimate(
        double distanceMetres,
        double durationSeconds,
        List<List<Double>> coordinates) {

    public RouteEstimate {
        coordinates = List.copyOf(coordinates);
    }
}
