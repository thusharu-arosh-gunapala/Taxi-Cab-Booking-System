package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.DriverLocationUpdateRequest;
import com.serendib.taxibooking.service.DriverLocationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/integrations/drivers")
public class DriverLocationIntegrationController {

    private final DriverLocationService driverLocationService;
    private final String integrationToken;

    public DriverLocationIntegrationController(
            DriverLocationService driverLocationService,
            @Value("${app.integrations.driver-token:}") String integrationToken) {
        this.driverLocationService = driverLocationService;
        this.integrationToken = integrationToken;
    }

    @PostMapping("/{driverId}/location")
    public void updateLocation(@PathVariable Long driverId,
                               @RequestHeader(name = "X-Integration-Token", required = false) String token,
                               @Valid @RequestBody DriverLocationUpdateRequest request) {
        verifyToken(token);
        driverLocationService.updateLocation(driverId, request);
    }

    private void verifyToken(String token) {
        if (integrationToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Driver integration is not configured.");
        }
        if (token == null || !MessageDigest.isEqual(
                integrationToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid integration credentials.");
        }
    }
}
