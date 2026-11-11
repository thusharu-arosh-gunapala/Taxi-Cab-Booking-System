package com.serendib.taxibooking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.serendib.taxibooking.entity.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class StripePaymentGateway implements PaymentGateway {

    private static final String STRIPE_API = "https://api.stripe.com/v1";

    private final RestClient restClient;
    private final String secretKey;

    public StripePaymentGateway(RestClient.Builder restClientBuilder,
                                @Value("${app.payment.stripe-secret-key:}") String secretKey) {
        this.restClient = restClientBuilder.build();
        this.secretKey = secretKey;
    }

    @Override
    public boolean isConfigured() {
        return !secretKey.isBlank();
    }

    @Override
    public CheckoutSession createCheckoutSession(Payment payment, String successUrl, String cancelUrl) {
        requireConfigured();
        long amountMinor;
        try {
            amountMinor = payment.getAmount().movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("The payment amount cannot be represented in the selected currency.");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("success_url", successUrl);
        form.add("cancel_url", cancelUrl);
        form.add("client_reference_id", payment.getPaymentReference());
        form.add("line_items[0][price_data][currency]", payment.getCurrency().toLowerCase(Locale.ROOT));
        form.add("line_items[0][price_data][unit_amount]", Long.toString(amountMinor));
        form.add("line_items[0][price_data][product_data][name]", "Taxi booking " + payment.getBooking().getBookingReference());
        form.add("line_items[0][quantity]", "1");
        form.add("metadata[payment_reference]", payment.getPaymentReference());
        form.add("metadata[booking_reference]", payment.getBooking().getBookingReference());

        try {
            JsonNode response = restClient.post()
                    .uri(STRIPE_API + "/checkout/sessions")
                    .headers(headers -> authorize(headers,
                            payment.getPaymentReference() + "-" + payment.getCheckoutAttempt()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.hasNonNull("id") || !response.hasNonNull("url")) {
                throw new PaymentProviderException("Stripe did not return a valid checkout session.");
            }
            return new CheckoutSession(
                    response.path("id").asText(),
                    response.path("url").asText(),
                    response.path("payment_status").asText("unpaid"),
                    payment.getPaymentReference(),
                    payment.getCurrency(),
                    amountMinor,
                    null);
        } catch (RestClientException exception) {
            throw new PaymentProviderException("Could not start Stripe Checkout. Please try again later.", exception);
        }
    }

    @Override
    public CheckoutSession retrieveCheckoutSession(String sessionId) {
        requireConfigured();
        try {
            JsonNode response = restClient.get()
                    .uri(STRIPE_API + "/checkout/sessions/{id}", sessionId)
                    .headers(headers -> authorize(headers, null))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.hasNonNull("id")) {
                throw new PaymentProviderException("Stripe did not return the requested checkout session.");
            }
            String paymentIntent = response.path("payment_intent").asText(null);
            return new CheckoutSession(
                    response.path("id").asText(),
                    response.path("url").asText(null),
                    response.path("payment_status").asText("unpaid"),
                    response.path("metadata").path("payment_reference").asText(null),
                    response.path("currency").asText("").toUpperCase(),
                    response.path("amount_total").isNumber()
                            ? response.path("amount_total").longValue()
                            : null,
                    paymentIntent);
        } catch (RestClientException exception) {
            throw new PaymentProviderException("Could not verify the Stripe checkout session.", exception);
        }
    }

    private void authorize(HttpHeaders headers, String idempotencyKey) {
        headers.setBasicAuth(secretKey, "");
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new PaymentProviderException("Online payment is not configured. Choose cash or contact support.");
        }
    }
}
