package com.serendib.taxibooking.dto;

import com.serendib.taxibooking.model.VehicleCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class DriverRegistrationForm {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Pattern(regexp = "^[0-9+() .-]{7,32}$", message = "Enter a valid phone number.")
    private String phone;

    @NotBlank
    @Pattern(regexp = "^(?:[0-9]{9}[VvXx]|[0-9]{12})$", message = "Enter a valid NIC number.")
    private String nicNumber;

    @NotBlank
    @Size(max = 255)
    private String address;

    @NotNull
    @Past(message = "Date of birth must be in the past.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    @NotBlank
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[A-Za-z0-9 -]+$", message = "Use letters, numbers, spaces, or hyphens.")
    private String licenseNumber;

    @NotNull
    @Future(message = "Driving licence must not be expired.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate licenseExpiryDate;

    @NotNull(message = "Choose a vehicle category.")
    private VehicleCategory vehicleCategory;

    @NotBlank
    @Size(max = 120)
    private String vehicleModel;

    @NotBlank
    @Size(max = 80)
    private String vehicleBrand;

    @NotNull
    @Min(1980)
    @Max(2100)
    private Integer vehicleYear;

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^[A-Za-z0-9 -]{2,20}$", message = "Enter a valid registration plate.")
    private String plateNumber;

    @NotBlank
    @Size(max = 40)
    private String vehicleColor;

    private MultipartFile profilePhoto;
    private MultipartFile licenseImage;
    private MultipartFile vehiclePhoto;
    private MultipartFile vehicleRegistrationDocument;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNicNumber() {
        return nicNumber;
    }

    public void setNicNumber(String nicNumber) {
        this.nicNumber = nicNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public LocalDate getLicenseExpiryDate() {
        return licenseExpiryDate;
    }

    public void setLicenseExpiryDate(LocalDate licenseExpiryDate) {
        this.licenseExpiryDate = licenseExpiryDate;
    }

    public VehicleCategory getVehicleCategory() {
        return vehicleCategory;
    }

    public void setVehicleCategory(VehicleCategory vehicleCategory) {
        this.vehicleCategory = vehicleCategory;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getVehicleBrand() {
        return vehicleBrand;
    }

    public void setVehicleBrand(String vehicleBrand) {
        this.vehicleBrand = vehicleBrand;
    }

    public Integer getVehicleYear() {
        return vehicleYear;
    }

    public void setVehicleYear(Integer vehicleYear) {
        this.vehicleYear = vehicleYear;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getVehicleColor() {
        return vehicleColor;
    }

    public void setVehicleColor(String vehicleColor) {
        this.vehicleColor = vehicleColor;
    }

    public MultipartFile getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(MultipartFile profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public MultipartFile getLicenseImage() {
        return licenseImage;
    }

    public void setLicenseImage(MultipartFile licenseImage) {
        this.licenseImage = licenseImage;
    }

    public MultipartFile getVehiclePhoto() {
        return vehiclePhoto;
    }

    public void setVehiclePhoto(MultipartFile vehiclePhoto) {
        this.vehiclePhoto = vehiclePhoto;
    }

    public MultipartFile getVehicleRegistrationDocument() {
        return vehicleRegistrationDocument;
    }

    public void setVehicleRegistrationDocument(MultipartFile vehicleRegistrationDocument) {
        this.vehicleRegistrationDocument = vehicleRegistrationDocument;
    }
}
