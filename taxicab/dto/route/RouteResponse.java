package com.taxicab.dto.route;

import com.taxicab.dto.location.LocationRequest;
import java.time.Instant;
import java.util.List;

public class RouteResponse {
    private String id;
    private LocationRequest pickup;
    private LocationRequest destination;
    private double roadDistanceMetres;
    private double roadDistanceKilometres;
    private double durationSeconds;
    private double durationMinutes;
    private List<List<Double>> geometry;
    private Instant calculatedAt;

    public RouteResponse() {
    }

    public RouteResponse(String id, LocationRequest pickup, LocationRequest destination, double roadDistanceMetres,
                         double durationSeconds, List<List<Double>> geometry, Instant calculatedAt) {
        this.id = id;
        this.pickup = pickup;
        this.destination = destination;
        this.roadDistanceMetres = roadDistanceMetres;
        this.roadDistanceKilometres = roadDistanceMetres / 1000.0;
        this.durationSeconds = durationSeconds;
        this.durationMinutes = durationSeconds / 60.0;
        this.geometry = geometry;
        this.calculatedAt = calculatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public double getRoadDistanceMetres() {
        return roadDistanceMetres;
    }

    public void setRoadDistanceMetres(double roadDistanceMetres) {
        this.roadDistanceMetres = roadDistanceMetres;
        this.roadDistanceKilometres = roadDistanceMetres / 1000.0;
    }

    public double getRoadDistanceKilometres() {
        return roadDistanceKilometres;
    }

    public void setRoadDistanceKilometres(double roadDistanceKilometres) {
        this.roadDistanceKilometres = roadDistanceKilometres;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = durationSeconds;
        this.durationMinutes = durationSeconds / 60.0;
    }

    public double getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(double durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public List<List<Double>> getGeometry() {
        return geometry;
    }

    public void setGeometry(List<List<Double>> geometry) {
        this.geometry = geometry;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
