package com.taxicab.dto.location;

public class LocationResponse {
    private Double latitude;
    private Double longitude;
    private String address;
    private String formattedAddress;

    public LocationResponse() {
    }

    public LocationResponse(Double latitude, Double longitude, String address, String formattedAddress) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.formattedAddress = formattedAddress;
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

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }
}
