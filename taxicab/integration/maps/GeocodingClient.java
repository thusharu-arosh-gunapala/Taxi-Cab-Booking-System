package com.taxicab.integration.maps;

import com.taxicab.dto.location.LocationSuggestionResponse;

import java.util.List;

public interface GeocodingClient {
    List<LocationSuggestionResponse> search(String query);
    LocationSuggestionResponse reverseGeocode(double latitude, double longitude);
}
