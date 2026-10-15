package com.taxicab.dto.location;

public class LocationSuggestionResponse {
    private String id;
    private String name;
    private String formattedAddress;
    private Double latitude;
    private Double longitude;

    public LocationSuggestionResponse() {
    }

    public LocationSuggestionResponse(String id, String name, String formattedAddress, Double latitude, Double longitude) {
        this.id = id;
        this.name = name;
        this.formattedAddress = formattedAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
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
}
