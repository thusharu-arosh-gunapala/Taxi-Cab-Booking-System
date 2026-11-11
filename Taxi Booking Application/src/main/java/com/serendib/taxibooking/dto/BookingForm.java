package com.serendib.taxibooking.dto;

import com.serendib.taxibooking.model.VehicleCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class BookingForm {

    @NotBlank
    @Size(max = 255)
    private String pickupAddress;

    @NotBlank
    @Size(max = 255)
    private String destinationAddress;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double pickupLatitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double pickupLongitude;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double destinationLatitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double destinationLongitude;

    @NotNull
    private VehicleCategory vehicleCategory;

    private boolean peakHour;

    @FutureOrPresent(message = "A scheduled booking must be in the future.")
    private LocalDateTime bookingTime;

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public Double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(Double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public Double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(Double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(Double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(Double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
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

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }
}
