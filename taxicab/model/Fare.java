package com.taxicab.model;

import java.time.Instant;

public class Fare {
    private Long routeId;
    private VehicleCategory vehicleCategory;
    private double baseFare;
    private double distanceCharge;
    private double applicableCharges;
    private double estimatedFare;
    private String currency;
    private Instant calculatedAt;

    public Fare() {
    }

    public Fare(Long routeId, VehicleCategory vehicleCategory, double baseFare, double distanceCharge,
               double applicableCharges, double estimatedFare, String currency, Instant calculatedAt) {
        this.routeId = routeId;
        this.vehicleCategory = vehicleCategory;
        this.baseFare = baseFare;
        this.distanceCharge = distanceCharge;
        this.applicableCharges = applicableCharges;
        this.estimatedFare = estimatedFare;
        this.currency = currency;
        this.calculatedAt = calculatedAt;
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

    public double getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(double baseFare) {
        this.baseFare = baseFare;
    }

    public double getDistanceCharge() {
        return distanceCharge;
    }

    public void setDistanceCharge(double distanceCharge) {
        this.distanceCharge = distanceCharge;
    }

    public double getApplicableCharges() {
        return applicableCharges;
    }

    public void setApplicableCharges(double applicableCharges) {
        this.applicableCharges = applicableCharges;
    }

    public double getEstimatedFare() {
        return estimatedFare;
    }

    public void setEstimatedFare(double estimatedFare) {
        this.estimatedFare = estimatedFare;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
