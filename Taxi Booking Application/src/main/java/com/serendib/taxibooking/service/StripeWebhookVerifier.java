package com.serendib.taxibooking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class StripeWebhookVerifier {

    private final String webhookSecret;
    private final long toleranceSeconds;

    public StripeWebhookVerifier(
            @Value("${app.payment.stripe-webhook-secret:}") String webhookSecret,
            @Value("${app.payment.webhook-tolerance-seconds:300}") long toleranceSeconds) {
        this.webhookSecret = webhookSecret;
        this.toleranceSeconds = toleranceSeconds;
    }

    public boolean isConfigured() {
        return !webhookSecret.isBlank();
    }

    public boolean isValid(String payload, String signatureHeader) {
        if (!isConfigured() || payload == null || signatureHeader == null) {
            return false;
        }
        Long timestamp = null;
        List<String> signatures = new ArrayList<>();
        for (String part : signatureHeader.split(",")) {
            String[] value = part.split("=", 2);
            if (value.length != 2) {
                continue;
            }
            if ("t".equals(value[0])) {
                try {
                    timestamp = Long.parseLong(value[1]);
                } catch (NumberFormatException exception) {
                    return false;
                }
            } else if ("v1".equals(value[0])) {
                signatures.add(value[1]);
            }
        }
        if (timestamp == null || signatures.isEmpty()
                || Math.abs(Instant.now().getEpochSecond() - timestamp) > toleranceSeconds) {
            return false;
        }
        byte[] expected = sign(timestamp + "." + payload);
        for (String signature : signatures) {
            try {
                if (MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature))) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
        return false;
    }

    private byte[] sign(String signedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Could not verify the Stripe webhook signature.", exception);
        }
    }
}
