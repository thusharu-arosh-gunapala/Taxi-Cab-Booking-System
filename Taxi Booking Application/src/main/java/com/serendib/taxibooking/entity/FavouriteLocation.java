package com.serendib.taxibooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "favourite_locations")
public class FavouriteLocation {

    public enum Type {
        HOME,
        WORK,
        OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 16)
    private Type type;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    protected FavouriteLocation() {
    }

    public FavouriteLocation(Passenger passenger, Type type, String address, double latitude, double longitude) {
        this.passenger = passenger;
        this.type = type;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public Type getType() {
        return type;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
