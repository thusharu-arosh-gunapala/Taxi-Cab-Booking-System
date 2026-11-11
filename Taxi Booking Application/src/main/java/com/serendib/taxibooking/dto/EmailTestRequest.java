package com.serendib.taxibooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailTestRequest(
        @NotBlank @Email @Size(max = 254) String to,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 5000) String message) {
}
