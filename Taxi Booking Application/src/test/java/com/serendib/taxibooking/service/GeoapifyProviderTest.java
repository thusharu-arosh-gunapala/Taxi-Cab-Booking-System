package com.serendib.taxibooking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendib.taxibooking.model.RouteEstimate;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;

class GeoapifyProviderTest {

    @Test
    void geocodingReadsFormattedAddressAndLatitudeLongitude() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.geoapify.com/v1/geocode/search?text=38%20Upper%20Street&apiKey=test-key&limit=5&lang=en&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"results":[{"formatted":"38 Upper Street, London","lat":51.5,"lon":-0.1}]}
                        """, MediaType.APPLICATION_JSON));
        GeocodingService service = new GeocodingService(
                builder, new ObjectMapper(), "test-key", "https://api.geoapify.com");

        var result = service.search("38 Upper Street");

        assertThat(result).containsExactly(
                new GeocodingService.PlaceSuggestion("38 Upper Street, London", 51.5, -0.1));
        server.verify();
    }

    @Test
    void routingReadsGeoapifyDistanceTimeAndGeoJsonCoordinates() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.geoapify.com/v1/routing?waypoints=6.9,79.8%7C6.92,79.85&mode=drive&format=geojson&apiKey=test-key"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"features":[{"properties":{"distance":3200,"time":720},
                         "geometry":{"type":"LineString","coordinates":[[79.8,6.9],[79.85,6.92]]}}]}
                        """, MediaType.APPLICATION_JSON));
        GeoapifyRoutingProvider provider = new GeoapifyRoutingProvider(
                builder, new ObjectMapper(), "test-key", "https://api.geoapify.com");

        RouteEstimate route = provider.route(79.8, 6.9, 79.85, 6.92);

        assertThat(route.distanceMetres()).isEqualTo(3200);
        assertThat(route.durationSeconds()).isEqualTo(720);
        assertThat(route.coordinates()).containsExactly(List.of(79.8, 6.9), List.of(79.85, 6.92));
        server.verify();
    }

    @Test
    void routingFlattensMultiLineStringRoadGeometry() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.geoapify.com/v1/routing?waypoints=6.9,79.8%7C6.92,79.85&mode=drive&format=geojson&apiKey=test-key"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"features":[{"properties":{"distance":3200,"time":720},
                         "geometry":{"type":"MultiLineString","coordinates":[[[79.8,6.9],[79.82,6.91]],[[79.82,6.91],[79.85,6.92]]]}}]}
                        """, MediaType.APPLICATION_JSON));
        GeoapifyRoutingProvider provider = new GeoapifyRoutingProvider(
                builder, new ObjectMapper(), "test-key", "https://api.geoapify.com");

        RouteEstimate route = provider.route(79.8, 6.9, 79.85, 6.92);

        assertThat(route.coordinates()).containsExactly(
                List.of(79.8, 6.9),
                List.of(79.82, 6.91),
                List.of(79.82, 6.91),
                List.of(79.85, 6.92));
        server.verify();
    }

    @Test
    void routingExplainsWhenGeoapifyCannotFindARoute() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.geoapify.com/v1/routing?waypoints=6.9,79.8%7C6.92,79.85&mode=drive&format=geojson&apiKey=test-key"))
                .andRespond(withSuccess("""
                        {"type":"FeatureCollection","features":[]}
                        """, MediaType.APPLICATION_JSON));
        GeoapifyRoutingProvider provider = new GeoapifyRoutingProvider(
                builder, new ObjectMapper(), "test-key", "https://api.geoapify.com");

        assertThatThrownBy(() -> provider.route(79.8, 6.9, 79.85, 6.92))
                .isInstanceOf(com.serendib.taxibooking.exception.ExternalProviderException.class)
                .hasMessageContaining("could not find a road route");
        server.verify();
    }

    @Test
    void routingExplainsGeoapifyAuthorizationErrorsWithoutHidingTheCause() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.geoapify.com/v1/routing?waypoints=6.9,79.8%7C6.92,79.85&mode=drive&format=geojson&apiKey=test-key"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Routing API is not enabled\"}"));
        GeoapifyRoutingProvider provider = new GeoapifyRoutingProvider(
                builder, new ObjectMapper(), "test-key", "https://api.geoapify.com");

        assertThatThrownBy(() -> provider.route(79.8, 6.9, 79.85, 6.92))
                .isInstanceOf(com.serendib.taxibooking.exception.ExternalProviderException.class)
                .hasMessageContaining("HTTP 403")
                .hasMessageContaining("Routing API is not enabled");
        server.verify();
    }
}
