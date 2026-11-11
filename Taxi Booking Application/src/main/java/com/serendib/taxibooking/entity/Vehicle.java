package com.serendib.taxibooking.entity;

import com.serendib.taxibooking.model.VehicleCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_category", nullable = false, length = 16)
    private VehicleCategory category;

    @Column(nullable = false, length = 120)
    private String model;

    @Column(nullable = false, length = 80)
    private String brand = "Unknown";

    @Column(name = "manufacturing_year", nullable = false)
    private int manufacturingYear = 2000;

    @Column(name = "plate_number", nullable = false, unique = true, length = 20)
    private String plateNumber;

    @Column(nullable = false, length = 40)
    private String color;

    @Column(name = "photo_key", length = 80)
    private String photoKey;

    @Column(name = "registration_document_key", length = 80)
    private String registrationDocumentKey;

    @Column(nullable = false)
    private boolean available;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    protected Vehicle() {
    }

    public Vehicle(VehicleCategory category, String model, String plateNumber, String color, Driver driver) {
        this(category, "Unknown", model, 2000, plateNumber, color, null, driver);
    }

    public Vehicle(VehicleCategory category, String brand, String model, int manufacturingYear,
                   String plateNumber, String color, String photoKey, Driver driver) {
        this.category = category;
        this.brand = brand;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.plateNumber = plateNumber;
        this.color = color;
        this.photoKey = photoKey;
        this.driver = driver;
        this.available = true;
    }

    public void update(VehicleCategory category, String model, String plateNumber, String color, Driver driver) {
        this.category = category;
        this.model = model;
        this.plateNumber = plateNumber;
        this.color = color;
        this.driver = driver;
    }

    public void toggleAvailability() {
        available = !available;
    }

    public Long getId() {
        return id;
    }

    public VehicleCategory getCategory() {
        return category;
    }

    public String getModel() {
        return model;
    }

    public String getBrand() {
        return brand;
    }

    public int getManufacturingYear() {
        return manufacturingYear;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public String getColor() {
        return color;
    }

    public String getPhotoKey() {
        return photoKey;
    }

    public String getRegistrationDocumentKey() {
        return registrationDocumentKey;
    }

    public void setRegistrationDocumentKey(String registrationDocumentKey) {
        this.registrationDocumentKey = registrationDocumentKey;
    }

    public boolean isAvailable() {
        return available;
    }

    public Driver getDriver() {
        return driver;
    }
}
