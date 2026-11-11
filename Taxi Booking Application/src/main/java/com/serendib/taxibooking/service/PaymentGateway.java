package com.serendib.taxibooking.service;

import com.serendib.taxibooking.entity.Payment;

public interface PaymentGateway {

    boolean isConfigured();

    CheckoutSession createCheckoutSession(Payment payment, String successUrl, String cancelUrl);

    CheckoutSession retrieveCheckoutSession(String sessionId);

    record CheckoutSession(String sessionId, String url, String paymentStatus, String paymentReference,
                           String currency, Long amountMinor, String transactionReference) {
    }
}
