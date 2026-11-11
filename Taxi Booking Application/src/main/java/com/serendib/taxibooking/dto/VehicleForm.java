package com.serendib.taxibooking.dto;

import com.serendib.taxibooking.model.VehicleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VehicleForm {

    @NotNull(message = "Choose a vehicle category.")
    private VehicleCategory category;

    @NotBlank
    @Size(max = 120)
    private String model;

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^[A-Za-z0-9 -]{2,20}$", message = "Enter a valid registration plate.")
    private String plateNumber;

    @NotBlank
    @Size(max = 40)
    private String color;

    private Long driverId;

    public VehicleCategory getCategory() {
        return category;
    }

    public void setCategory(VehicleCategory category) {
        this.category = category;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }
}
