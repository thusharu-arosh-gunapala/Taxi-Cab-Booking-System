package com.serendib.taxibooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class MailtrapEmailApiClient {

    private static final Logger logger = LoggerFactory.getLogger(MailtrapEmailApiClient.class);
    private final RestClient restClient;
    private final String apiToken;
    private final String apiUrl;
    private final String fromAddress;

    public MailtrapEmailApiClient(RestClient.Builder restClientBuilder,
                                  @Value("${app.mail.mailtrap.api-token:}") String apiToken,
                                  @Value("${app.mail.mailtrap.api-url:https://send.api.mailtrap.io/api/send}") String apiUrl,
                                  @Value("${app.mail.from:no-reply@serendibride.local}") String fromAddress) {
        this.restClient = restClientBuilder.build();
        this.apiToken = apiToken;
        this.apiUrl = apiUrl;
        this.fromAddress = fromAddress;
    }

    public boolean isConfigured() {
        return apiToken != null && !apiToken.isBlank();
    }

    public boolean send(String toAddress, String subject, String text, String html) {
        if (!isConfigured()) {
            return false;
        }
        Map<String, Object> payload = Map.of(
                "from", Map.of("email", fromAddress, "name", "Serendib Ride"),
                "to", List.of(Map.of("email", toAddress)),
                "subject", subject,
                "text", text,
                "html", html);
        try {
            restClient.post()
                    .uri(apiUrl)
                    .headers(headers -> headers.setBearerAuth(apiToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException exception) {
            logger.warn("Mailtrap Email Sending API request failed.");
            return false;
        }
    }
}
