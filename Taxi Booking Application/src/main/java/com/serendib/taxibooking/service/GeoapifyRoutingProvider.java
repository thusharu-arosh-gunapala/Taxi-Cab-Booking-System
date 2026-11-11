package com.serendib.taxibooking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendib.taxibooking.exception.ExternalProviderException;
import com.serendib.taxibooking.model.RouteEstimate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class GeoapifyRoutingProvider implements RoutingProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;

    public GeoapifyRoutingProvider(RestClient.Builder restClientBuilder,
                                   ObjectMapper objectMapper,
                                   @Value("${app.maps.geoapify-api-key:}") String apiKey,
                                   @Value("${app.maps.geoapify-base-url:https://api.geoapify.com}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Override
    public RouteEstimate route(double pickupLongitude, double pickupLatitude,
                               double destinationLongitude, double destinationLatitude) {
        if (apiKey.isBlank()) {
            throw new ExternalProviderException("Road routing is not configured. Set GEOAPIFY_API_KEY.");
        }
        String waypoints = pickupLatitude + "," + pickupLongitude + "|"
                + destinationLatitude + "," + destinationLongitude;
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/v1/routing")
                .queryParam("waypoints", waypoints)
                .queryParam("mode", "drive")
                .queryParam("format", "geojson")
                .queryParam("apiKey", apiKey)
                .build()
                .encode()
                .toUri();
        try {
            String json = restClient.get().uri(uri).retrieve().body(String.class);
            if (json == null) {
                throw new ExternalProviderException("The routing service returned an empty response.");
            }
            JsonNode root = objectMapper.readTree(json);
            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) {
                String providerMessage = providerMessage(root);
                throw new ExternalProviderException(providerMessage == null
                        ? "Geoapify could not find a road route between these locations. Choose pickup and destination points connected by roads, then try again."
                        : "Geoapify could not find a road route: " + providerMessage);
            }

            JsonNode route = features.path(0);
            JsonNode properties = route.path("properties");
            double distance = properties.path("distance").asDouble(Double.NaN);
            double duration = properties.path("time").asDouble(Double.NaN);
            JsonNode routeGeometry = route.path("geometry");
            if (!Double.isFinite(distance) || distance <= 0
                    || !Double.isFinite(duration) || duration <= 0) {
                throw new ExternalProviderException(
                        "Geoapify returned a route without a valid distance or travel time. Check the Geoapify Routing API key and plan.");
            }
            String geometryType = routeGeometry.path("type").asText("");
            if (!geometryType.equals("LineString") && !geometryType.equals("MultiLineString")) {
                throw new ExternalProviderException(
                        "Geoapify returned route details with unsupported road geometry.");
            }

            List<List<Double>> geometry = new ArrayList<>();
            JsonNode routeCoordinates = routeGeometry.path("coordinates");
            if (!routeCoordinates.isArray()) {
                throw new ExternalProviderException("The routing service returned invalid route geometry.");
            }
            for (JsonNode coordinateOrSegment : routeCoordinates) {
                appendCoordinates(coordinateOrSegment, geometry);
            }
            if (geometry.size() < 2) {
                throw new ExternalProviderException(
                        "Geoapify returned route details but fewer than two road coordinates.");
            }
            return new RouteEstimate(distance, duration, List.copyOf(geometry));
        } catch (RestClientResponseException exception) {
            String providerMessage = providerMessage(exception.getResponseBodyAsString());
            String detail = providerMessage == null
                    ? "Geoapify returned HTTP " + exception.getStatusCode().value() + "."
                    : "Geoapify returned HTTP " + exception.getStatusCode().value() + ": " + providerMessage;
            throw new ExternalProviderException(
                    detail + " Check that GEOAPIFY_API_KEY is valid and has Routing API access.", exception);
        } catch (RestClientException exception) {
            throw new ExternalProviderException("The road routing service is temporarily unavailable.", exception);
        } catch (ExternalProviderException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw new ExternalProviderException("The road routing service returned an invalid response.", exception);
        }
    }

    private void appendCoordinates(JsonNode node, List<List<Double>> coordinates) {
        if (!node.isArray() || node.isEmpty()) {
            throw new ExternalProviderException("The routing service returned invalid route geometry.");
        }
        if (node.get(0).isNumber()) {
            if (node.size() < 2 || !node.get(1).isNumber()) {
                throw new ExternalProviderException("The routing service returned invalid route geometry.");
            }
            double longitude = node.get(0).doubleValue();
            double latitude = node.get(1).doubleValue();
            if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180
                    || !Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
                throw new ExternalProviderException("The routing service returned out-of-range route coordinates.");
            }
            coordinates.add(List.of(longitude, latitude));
            return;
        }
        for (JsonNode child : node) {
            appendCoordinates(child, coordinates);
        }
    }

    private String providerMessage(String responseBody) {
        try {
            return providerMessage(objectMapper.readTree(responseBody));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return null;
        }
    }

    private String providerMessage(JsonNode root) {
        if (root == null || !root.isObject()) {
            return null;
        }
        JsonNode message = root.path("message");
        if (message.isTextual() && !message.asText().isBlank()) {
            return limitMessage(message.asText());
        }
        JsonNode error = root.path("error");
        if (error.isTextual() && !error.asText().isBlank()) {
            return limitMessage(error.asText());
        }
        JsonNode errorMessage = error.path("message");
        return errorMessage.isTextual() && !errorMessage.asText().isBlank()
                ? limitMessage(errorMessage.asText())
                : null;
    }

    private String limitMessage(String message) {
        return message.length() <= 240 ? message : message.substring(0, 240);
    }
}
