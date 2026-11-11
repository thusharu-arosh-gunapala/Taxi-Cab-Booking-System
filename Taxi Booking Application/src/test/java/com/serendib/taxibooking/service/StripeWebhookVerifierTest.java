package com.serendib.taxibooking.service;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class StripeWebhookVerifierTest {

    private static final String SECRET = "whsec-test-secret";

    private final StripeWebhookVerifier verifier = new StripeWebhookVerifier(SECRET, 300);

    @Test
    void acceptsValidSignatureWithinTolerance() throws Exception {
        String payload = "{\"type\":\"checkout.session.completed\"}";
        long timestamp = Instant.now().getEpochSecond();

        assertThat(verifier.isValid(payload, signature(payload, timestamp))).isTrue();
    }

    @Test
    void rejectsModifiedPayloadAndStaleSignatures() throws Exception {
        String payload = "{\"type\":\"checkout.session.completed\"}";
        long timestamp = Instant.now().getEpochSecond();

        assertThat(verifier.isValid(payload + " ", signature(payload, timestamp))).isFalse();
        assertThat(verifier.isValid(payload, signature(payload, timestamp - 301))).isFalse();
    }

    private String signature(String payload, long timestamp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signedContent = timestamp + "." + payload;
        String digest = HexFormat.of().formatHex(mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + digest;
    }
}
