package com.taxicab.dto.tracking;

import java.time.Instant;

public class DriverLocationResponse {
    private Long bookingId;
    private Long driverId;
    private Double latitude;
    private Double longitude;
    private Instant locationTimestamp;
    private String status;

    public DriverLocationResponse() {
    }

    public DriverLocationResponse(Long bookingId, Long driverId, Double latitude, Double longitude,
                                Instant locationTimestamp, String status) {
        this.bookingId = bookingId;
        this.driverId = driverId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationTimestamp = locationTimestamp;
        this.status = status;
    }

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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Instant getLocationTimestamp() {
        return locationTimestamp;
    }

    public void setLocationTimestamp(Instant locationTimestamp) {
        this.locationTimestamp = locationTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
