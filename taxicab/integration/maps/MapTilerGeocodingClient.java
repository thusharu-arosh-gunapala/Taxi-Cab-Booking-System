package com.taxicab.integration.maps;

import com.taxicab.config.MapsConfig;
import com.taxicab.dto.location.LocationSuggestionResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
public class MapTilerGeocodingClient implements GeocodingClient {

    private final RestTemplate restTemplate;
    private final MapsConfig mapsConfig;

    public MapTilerGeocodingClient(RestTemplate restTemplate, MapsConfig mapsConfig) {
        this.restTemplate = restTemplate;
        this.mapsConfig = mapsConfig;
    }

    @Override
    public List<LocationSuggestionResponse> search(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        if (mapsConfig.getApiKey() == null || mapsConfig.getApiKey().isBlank()) {
            return List.of(new LocationSuggestionResponse("mock-1", query, query + ", Sri Lanka", 6.9271, 79.8612));
        }
        return Collections.emptyList();
    }

    @Override
    public LocationSuggestionResponse reverseGeocode(double latitude, double longitude) {
        if (mapsConfig.getApiKey() == null || mapsConfig.getApiKey().isBlank()) {
            return new LocationSuggestionResponse("mock-reverse", "Reverse geocode", "Latitude " + latitude + ", Longitude " + longitude, latitude, longitude);
        }
        return new LocationSuggestionResponse("mock-reverse", "MapTiler result", "Coordinate lookup", latitude, longitude);
    }
}
