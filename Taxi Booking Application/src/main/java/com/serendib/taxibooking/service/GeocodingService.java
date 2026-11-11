package com.serendib.taxibooking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.serendib.taxibooking.exception.ExternalProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeocodingService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;

    public GeocodingService(RestClient.Builder restClientBuilder,
                            ObjectMapper objectMapper,
                            @Value("${app.maps.geoapify-api-key:}") String apiKey,
                            @Value("${app.maps.geoapify-base-url:https://api.geoapify.com}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public List<PlaceSuggestion> search(String query) {
        if (apiKey.isBlank()) {
            throw new ExternalProviderException("Address search is not configured. Set GEOAPIFY_API_KEY.");
        }
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() < 3 || normalized.length() > 160) {
            throw new IllegalArgumentException("Search text must be between 3 and 160 characters.");
        }

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/v1/geocode/search")
                .queryParam("text", normalized)
                .queryParam("apiKey", apiKey)
                .queryParam("limit", 5)
                .queryParam("lang", "en")
                .queryParam("format", "json")
                .build()
                .encode()
                .toUri();
        try {
            String json = restClient.get().uri(uri).retrieve().body(String.class);
            if (json == null) {
                throw new ExternalProviderException("The address search service returned an empty response.");
            }
            JsonNode root = objectMapper.readTree(json);
            JsonNode results = root.path("results");
            if (!results.isArray()) {
                throw new ExternalProviderException("The geocoding service returned an invalid response.");
            }
            List<PlaceSuggestion> suggestions = new ArrayList<>();
            for (JsonNode result : results) {
                String address = result.path("formatted").asText("");
                JsonNode latitude = result.path("lat");
                JsonNode longitude = result.path("lon");
                if (!latitude.isNumber() || !longitude.isNumber() || address.isBlank()) {
                    continue;
                }
                suggestions.add(new PlaceSuggestion(
                        address,
                        latitude.doubleValue(),
                        longitude.doubleValue()));
            }
            return List.copyOf(suggestions);
        } catch (RestClientException exception) {
            throw new ExternalProviderException("The address search service is temporarily unavailable.", exception);
        } catch (ExternalProviderException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw new ExternalProviderException("The address search service returned an invalid response.", exception);
        }
    }

    public record PlaceSuggestion(String address, double latitude, double longitude) {
    }
}
