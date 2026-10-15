package com.taxicab.service;

import com.taxicab.dto.location.LocationRequest;
import com.taxicab.dto.location.LocationResponse;
import com.taxicab.dto.location.LocationSuggestionResponse;
import com.taxicab.geo.CoordinateValidator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerLocationService {

    private final GeocodingService geocodingService;

    public CustomerLocationService(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    public List<LocationSuggestionResponse> searchLocations(String query) {
        return geocodingService.searchLocations(query);
    }

    public LocationResponse reverseGeocode(LocationRequest location) {
        CoordinateValidator.validateLocationRequest(location);
        return geocodingService.reverseGeocode(location.getLatitude(), location.getLongitude());
    }
}
