package com.taxicab.model;

import java.time.Instant;

public class Booking {
    private Long id;
    private Long customerId;
    private Long driverId;
    private String status;
    private String vehicleCategory;
    private Long routeId;
    private Instant createdAt;
    private Instant updatedAt;

    public Booking() {
    }

    public Booking(Long id, Long customerId, Long driverId, String status, String vehicleCategory, Long routeId) {
        this.id = id;
        this.customerId = customerId;
        this.driverId = driverId;
        this.status = status;
        this.vehicleCategory = vehicleCategory;
        this.routeId = routeId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVehicleCategory() {
        return vehicleCategory;
    }

    public void setVehicleCategory(String vehicleCategory) {
        this.vehicleCategory = vehicleCategory;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
