package com.taxicab.geo;

import com.taxicab.dto.location.LocationRequest;
import com.taxicab.exception.InvalidLocationException;

public final class CoordinateValidator {

    private CoordinateValidator() {
    }

    public static void validateLatitude(double latitude) {
        if (latitude < -90 || latitude > 90) {
            throw new InvalidLocationException("Latitude must be between -90 and 90 degrees.");
        }
    }

    public static void validateLongitude(double longitude) {
        if (longitude < -180 || longitude > 180) {
            throw new InvalidLocationException("Longitude must be between -180 and 180 degrees.");
        }
    }

    public static void validateLocationRequest(LocationRequest location) {
        if (location == null) {
            throw new InvalidLocationException("Location is required.");
        }
        if (location.getLatitude() == null || location.getLongitude() == null) {
            throw new InvalidLocationException("Latitude and longitude must both be provided.");
        }
        validateLatitude(location.getLatitude());
        validateLongitude(location.getLongitude());
    }
}
