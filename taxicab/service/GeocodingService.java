package com.taxicab.service;

import com.taxicab.dto.location.LocationResponse;
import com.taxicab.dto.location.LocationSuggestionResponse;
import com.taxicab.exception.InvalidLocationException;
import com.taxicab.geo.CoordinateValidator;
import com.taxicab.integration.maps.GeocodingClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GeocodingService {

    private final GeocodingClient geocodingClient;

    public GeocodingService(GeocodingClient geocodingClient) {
        this.geocodingClient = geocodingClient;
    }

    public List<LocationSuggestionResponse> searchLocations(String query) {
        if (query == null || query.isBlank()) {
            throw new InvalidLocationException("Search query is required.");
        }
        return geocodingClient.search(query);
    }

    public LocationResponse reverseGeocode(double latitude, double longitude) {
        CoordinateValidator.validateLatitude(latitude);
        CoordinateValidator.validateLongitude(longitude);
        LocationSuggestionResponse response = geocodingClient.reverseGeocode(latitude, longitude);
        return new LocationResponse(
                response.getLatitude(),
                response.getLongitude(),
                response.getName(),
                response.getFormattedAddress()
        );
    }
}
