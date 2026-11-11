package com.serendib.taxibooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CashCollectionRequest(
        @NotBlank @Size(max = 255) String collectionReference) {
}
