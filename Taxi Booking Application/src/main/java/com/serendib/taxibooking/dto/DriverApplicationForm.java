package com.serendib.taxibooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DriverApplicationForm {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Pattern(regexp = "^[0-9+() .-]{7,32}$", message = "Enter a valid phone number.")
    private String phone;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[A-Za-z0-9 -]+$", message = "Use letters, numbers, spaces, or hyphens.")
    private String licenseNumber;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }
}
