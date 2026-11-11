package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.EmailTestRequest;
import com.serendib.taxibooking.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@Profile("dev")
public class EmailTestController {

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestEmail(@Valid @RequestBody EmailTestRequest request) {
        EmailService.DeliveryResult result = emailService.sendTestEmail(
                request.to(), request.subject(), request.message());
        return switch (result) {
            case SENT -> ResponseEntity.ok(Map.of("status", "sent"));
            case NOT_CONFIGURED -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "not_configured"));
            case FAILED -> ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("status", "delivery_failed"));
        };
    }
}
