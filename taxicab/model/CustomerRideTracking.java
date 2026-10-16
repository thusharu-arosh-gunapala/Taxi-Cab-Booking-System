package com.taxicab.model;

import java.time.Instant;

public class CustomerRideTracking {
    private Long bookingId;
    private Long driverId;
    private String bookingStatus;
    private Double driverLatitude;
    private Double driverLongitude;
    private Instant locationTimestamp;
    private Double speedKph;
    private Double distanceToPickupKm;
    private Double etaToPickupMinutes;
    private Double distanceTravelledKm;
    private Double distanceRemainingKm;
    private Double tripEtaMinutes;
    private Double elapsedMinutes;
    private Double averageSpeedKph;

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public Double getDriverLatitude() {
        return driverLatitude;
    }

    public void setDriverLatitude(Double driverLatitude) {
        this.driverLatitude = driverLatitude;
    }

    public Double getDriverLongitude() {
        return driverLongitude;
    }

    public void setDriverLongitude(Double driverLongitude) {
        this.driverLongitude = driverLongitude;
    }

    public Instant getLocationTimestamp() {
        return locationTimestamp;
    }

    public void setLocationTimestamp(Instant locationTimestamp) {
        this.locationTimestamp = locationTimestamp;
    }

    public Double getSpeedKph() {
        return speedKph;
    }

    public void setSpeedKph(Double speedKph) {
        this.speedKph = speedKph;
    }

    public Double getDistanceToPickupKm() {
        return distanceToPickupKm;
    }

    public void setDistanceToPickupKm(Double distanceToPickupKm) {
        this.distanceToPickupKm = distanceToPickupKm;
    }

    public Double getEtaToPickupMinutes() {
        return etaToPickupMinutes;
    }

    public void setEtaToPickupMinutes(Double etaToPickupMinutes) {
        this.etaToPickupMinutes = etaToPickupMinutes;
    }

    public Double getDistanceTravelledKm() {
        return distanceTravelledKm;
    }

    public void setDistanceTravelledKm(Double distanceTravelledKm) {
        this.distanceTravelledKm = distanceTravelledKm;
    }

    public Double getDistanceRemainingKm() {
        return distanceRemainingKm;
    }

    public void setDistanceRemainingKm(Double distanceRemainingKm) {
        this.distanceRemainingKm = distanceRemainingKm;
    }

    public Double getTripEtaMinutes() {
        return tripEtaMinutes;
    }

    public void setTripEtaMinutes(Double tripEtaMinutes) {
        this.tripEtaMinutes = tripEtaMinutes;
    }

    public Double getElapsedMinutes() {
        return elapsedMinutes;
    }

    public void setElapsedMinutes(Double elapsedMinutes) {
        this.elapsedMinutes = elapsedMinutes;
    }

    public Double getAverageSpeedKph() {
        return averageSpeedKph;
    }

    public void setAverageSpeedKph(Double averageSpeedKph) {
        this.averageSpeedKph = averageSpeedKph;
    }
}
