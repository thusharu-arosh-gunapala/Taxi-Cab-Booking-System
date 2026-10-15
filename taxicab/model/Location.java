package com.taxicab.model;

import com.taxicab.exception.InvalidLocationException;

public class Location {
    private Double latitude;
    private Double longitude;
    private String address;

    public Location() {
    }

    public Location(Double latitude, Double longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        validate();
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void validate() {
        if (latitude == null || longitude == null) {
            throw new InvalidLocationException("Latitude and longitude are required.");
        }
        if (latitude < -90 || latitude > 90) {
            throw new InvalidLocationException("Latitude must be between -90 and 90 degrees.");
        }
        if (longitude < -180 || longitude > 180) {
            throw new InvalidLocationException("Longitude must be between -180 and 180 degrees.");
        }
    }
}
