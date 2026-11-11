package com.serendib.taxibooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DriverAssignmentRequest(
        @NotBlank @Size(max = 120) String driverReference) {
}
