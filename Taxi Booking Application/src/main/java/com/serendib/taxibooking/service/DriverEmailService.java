package com.serendib.taxibooking.service;

import com.serendib.taxibooking.entity.Driver;
import org.springframework.stereotype.Service;

@Service
public class DriverEmailService {

    private final EmailService emailService;

    public DriverEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    public DeliveryResult sendPasswordReset(Driver driver, String token) {
        return toDeliveryResult(emailService.sendPasswordResetEmail(driver, token));
    }

    private DeliveryResult toDeliveryResult(EmailService.DeliveryResult result) {
        return switch (result) {
            case SENT -> DeliveryResult.SENT;
            case NOT_CONFIGURED -> DeliveryResult.NOT_CONFIGURED;
            case FAILED -> DeliveryResult.FAILED;
        };
    }

    public enum DeliveryResult {
        SENT,
        NOT_CONFIGURED,
        FAILED
    }
}
