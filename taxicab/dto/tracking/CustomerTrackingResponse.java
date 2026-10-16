package com.taxicab.dto.tracking;

import java.time.Instant;

public class CustomerTrackingResponse {
    private Long bookingId;
    private String bookingStatus;
    private Long driverId;
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

    public CustomerTrackingResponse() {
    }

    public CustomerTrackingResponse(Long bookingId, String bookingStatus, Long driverId, Double driverLatitude,
                                   Double driverLongitude, Instant locationTimestamp, Double speedKph,
                                   Double distanceToPickupKm, Double etaToPickupMinutes, Double distanceTravelledKm,
                                   Double distanceRemainingKm, Double tripEtaMinutes, Double elapsedMinutes,
                                   Double averageSpeedKph) {
        this.bookingId = bookingId;
        this.bookingStatus = bookingStatus;
        this.driverId = driverId;
        this.driverLatitude = driverLatitude;
        this.driverLongitude = driverLongitude;
        this.locationTimestamp = locationTimestamp;
        this.speedKph = speedKph;
        this.distanceToPickupKm = distanceToPickupKm;
        this.etaToPickupMinutes = etaToPickupMinutes;
        this.distanceTravelledKm = distanceTravelledKm;
        this.distanceRemainingKm = distanceRemainingKm;
        this.tripEtaMinutes = tripEtaMinutes;
        this.elapsedMinutes = elapsedMinutes;
        this.averageSpeedKph = averageSpeedKph;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
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
