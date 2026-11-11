package com.serendib.taxibooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RefundForm {

    @NotBlank(message = "A reason is required.")
    @Size(max = 500, message = "The reason can be up to 500 characters.")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
