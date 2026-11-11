package com.serendib.taxibooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DriverPasswordResetForm {

    @NotBlank
    @Size(min = 12, max = 72, message = "Use between 12 and 72 characters.")
    private String password;

    @NotBlank
    private String confirmPassword;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
