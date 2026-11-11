package com.serendib.taxibooking.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendib.taxibooking.service.PaymentGateway;
import com.serendib.taxibooking.service.PaymentService;
import com.serendib.taxibooking.service.PaymentProviderException;
import com.serendib.taxibooking.service.StripeWebhookVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payments/stripe")
public class StripeWebhookController {

    private final StripeWebhookVerifier webhookVerifier;
    private final PaymentGateway paymentGateway;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public StripeWebhookController(StripeWebhookVerifier webhookVerifier,
                                   PaymentGateway paymentGateway,
                                   PaymentService paymentService,
                                   ObjectMapper objectMapper) {
        this.webhookVerifier = webhookVerifier;
        this.paymentGateway = paymentGateway;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                         @RequestHeader(name = "Stripe-Signature", required = false)
                                         String signature) {
        if (!webhookVerifier.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Stripe webhook verification is not configured.");
        }
        if (!webhookVerifier.isValid(payload, signature)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature.");
        }

        JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook payload.");
        }
        String eventType = event.path("type").asText();
        String sessionId = event.path("data").path("object").path("id").asText(null);
        if (sessionId == null || sessionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe webhook has no session reference.");
        }

        if ("checkout.session.completed".equals(eventType)) {
            PaymentGateway.CheckoutSession session = paymentGateway.retrieveCheckoutSession(sessionId);
            paymentService.confirmStripePayment(session);
        } else if ("checkout.session.async_payment_succeeded".equals(eventType)) {
            PaymentGateway.CheckoutSession session = paymentGateway.retrieveCheckoutSession(sessionId);
            paymentService.confirmStripePayment(session);
        } else if ("checkout.session.expired".equals(eventType)) {
            paymentService.failCheckoutSession(sessionId);
        } else if ("checkout.session.async_payment_failed".equals(eventType)) {
            paymentService.failCheckoutSession(sessionId);
        }
        return ResponseEntity.ok().build();
    }
}
