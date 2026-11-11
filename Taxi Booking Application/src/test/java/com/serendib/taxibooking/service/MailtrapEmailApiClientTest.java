package com.serendib.taxibooking.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

class MailtrapEmailApiClientTest {

    @Test
    void sendsBrandedJsonEmailWithBearerAuthorization() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MailtrapEmailApiClient client = new MailtrapEmailApiClient(
                builder,
                "test-api-token",
                "https://mailtrap.test/api/send",
                "no-reply@example.test");
        server.expect(requestTo("https://mailtrap.test/api/send"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "from": {"email": "no-reply@example.test", "name": "Serendib Ride"},
                          "to": [{"email": "driver@example.test"}],
                          "subject": "Driver approval",
                          "text": "Temporary password: private",
                          "html": "<p>Temporary password: private</p>"
                        }
                        """))
                .andRespond(withSuccess());

        assertThat(client.send(
                "driver@example.test",
                "Driver approval",
                "Temporary password: private",
                "<p>Temporary password: private</p>")).isTrue();
        server.verify();
    }

    @Test
    void reportsUnauthorizedApiResponseAsDeliveryFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MailtrapEmailApiClient client = new MailtrapEmailApiClient(
                builder,
                "invalid-test-token",
                "https://mailtrap.test/api/send",
                "no-reply@example.test");
        server.expect(requestTo("https://mailtrap.test/api/send"))
                .andRespond(withStatus(UNAUTHORIZED));

        assertThat(client.send("driver@example.test", "Subject", "Text", "<p>HTML</p>")).isFalse();
        server.verify();
    }

    @Test
    void refusesToCallApiWithoutToken() {
        MailtrapEmailApiClient client = new MailtrapEmailApiClient(
                RestClient.builder(), "", "https://mailtrap.test/api/send", "no-reply@example.test");

        assertThat(client.isConfigured()).isFalse();
        assertThat(client.send("driver@example.test", "Subject", "Text", "<p>HTML</p>")).isFalse();
    }
}
