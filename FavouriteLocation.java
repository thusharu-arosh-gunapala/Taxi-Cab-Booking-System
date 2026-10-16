package com.taxibooking.model; //FavouriteLocation

/** Stored in favourite_locations.txt as: locationId|passengerId|type|address|latitude|longitude */
public class FavouriteLocation {
    public enum Type { HOME, WORK, OTHER }

    private String locationId;
    private String passengerId;
    private Type type;
    private String address;
    private double latitude;
    private double longitude;

    public FavouriteLocation() { }

    public String toFileLine() {
        return String.join("|", User.clean(locationId), User.clean(passengerId), type.name(),
                User.clean(address), String.valueOf(latitude), String.valueOf(longitude));
    }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}

