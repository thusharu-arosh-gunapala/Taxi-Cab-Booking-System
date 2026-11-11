package com.serendib.taxibooking.entity;

import com.serendib.taxibooking.model.VehicleCategory;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false, unique = true, length = 45)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @Column(name = "pickup_address", nullable = false, length = 255)
    private String pickupAddress;

    @Column(name = "destination_address", nullable = false, length = 255)
    private String destinationAddress;

    @Column(name = "pickup_latitude", nullable = false)
    private double pickupLatitude;

    @Column(name = "pickup_longitude", nullable = false)
    private double pickupLongitude;

    @Column(name = "destination_latitude", nullable = false)
    private double destinationLatitude;

    @Column(name = "destination_longitude", nullable = false)
    private double destinationLongitude;

    @Column(name = "estimated_distance_km", nullable = false, precision = 10, scale = 3)
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private long estimatedDurationMinutes;

    @Column(name = "estimated_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedFare;

    @Column(name = "fare_currency", nullable = false, length = 3)
    private String fareCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_category", nullable = false, length = 16)
    private VehicleCategory vehicleCategory;

    @Column(name = "peak_hour", nullable = false)
    private boolean peakHour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BookingStatus status;

    @Column(name = "assigned_driver_reference", length = 120)
    private String assignedDriverReference;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY)
    private Trip trip;

    protected Booking() {
    }

    public Booking(String bookingReference, Passenger passenger, String pickupAddress, String destinationAddress,
                   double pickupLatitude, double pickupLongitude, double destinationLatitude,
                   double destinationLongitude, BigDecimal estimatedDistanceKm, BigDecimal estimatedFare,
                   long estimatedDurationMinutes, String fareCurrency, VehicleCategory vehicleCategory, boolean peakHour,
                   LocalDateTime bookingTime) {
        this.bookingReference = bookingReference;
        this.passenger = passenger;
        this.pickupAddress = pickupAddress;
        this.destinationAddress = destinationAddress;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.estimatedDistanceKm = estimatedDistanceKm;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.estimatedFare = estimatedFare;
        this.fareCurrency = fareCurrency;
        this.vehicleCategory = vehicleCategory;
        this.peakHour = peakHour;
        this.bookingTime = bookingTime;
        this.status = BookingStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String pickupAddress, String destinationAddress,
                       double pickupLatitude, double pickupLongitude,
                       double destinationLatitude, double destinationLongitude,
                       BigDecimal distanceKm, long durationMinutes,
                       BigDecimal fare, String currency, VehicleCategory category,
                       boolean peakHour, LocalDateTime bookingTime) {
        requireStatus(BookingStatus.PENDING, "Only pending bookings can be changed.");
        this.pickupAddress = pickupAddress;
        this.destinationAddress = destinationAddress;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.estimatedDistanceKm = distanceKm;
        this.estimatedDurationMinutes = durationMinutes;
        this.estimatedFare = fare;
        this.fareCurrency = currency;
        this.vehicleCategory = category;
        this.peakHour = peakHour;
        this.bookingTime = bookingTime;
        this.updatedAt = Instant.now();
    }

    public void beginDriverSearch() {
        requireStatus(BookingStatus.PENDING, "Only pending bookings can start driver matching.");
        status = BookingStatus.SEARCHING_FOR_DRIVER;
        updatedAt = Instant.now();
    }

    public void assignDriver(String driverReference) {
        if (status != BookingStatus.SEARCHING_FOR_DRIVER && status != BookingStatus.REJECTED) {
            throw new IllegalStateException("A driver can only be assigned while a booking is being matched.");
        }
        this.assignedDriverReference = requireText(driverReference, "Driver reference");
        status = BookingStatus.DRIVER_ASSIGNED;
        updatedAt = Instant.now();
    }

    public void respondToAssignment(boolean accepted) {
        requireStatus(BookingStatus.DRIVER_ASSIGNED, "There is no driver assignment waiting for a response.");
        status = accepted ? BookingStatus.ACCEPTED : BookingStatus.REJECTED;
        if (!accepted) {
            assignedDriverReference = null;
        }
        updatedAt = Instant.now();
    }

    public void markNoDriverAvailable() {
        if (status != BookingStatus.SEARCHING_FOR_DRIVER && status != BookingStatus.PENDING) {
            throw new IllegalStateException("This booking is not waiting for a driver.");
        }
        status = BookingStatus.NO_DRIVER_AVAILABLE;
        updatedAt = Instant.now();
    }

    public void markDriverArriving() {
        requireStatus(BookingStatus.ACCEPTED, "A driver must accept a booking before arriving.");
        status = BookingStatus.ARRIVING;
        updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (status != BookingStatus.PENDING && status != BookingStatus.SEARCHING_FOR_DRIVER
                && status != BookingStatus.DRIVER_ASSIGNED && status != BookingStatus.REJECTED
                && status != BookingStatus.NO_DRIVER_AVAILABLE) {
            throw new IllegalStateException("This booking can no longer be cancelled.");
        }
        cancellationReason = requireText(reason, "Cancellation reason");
        status = BookingStatus.CANCELLED;
        updatedAt = Instant.now();
    }

    public void startTrip() {
        if ((status != BookingStatus.ACCEPTED && status != BookingStatus.ARRIVING) || trip != null) {
            throw new IllegalStateException("A driver must accept the booking before a trip can start.");
        }
        status = BookingStatus.IN_PROGRESS;
        updatedAt = Instant.now();
    }

    public void completeTrip() {
        if (status != BookingStatus.IN_PROGRESS || trip == null) {
            throw new IllegalStateException("The trip must be in progress before it can be completed.");
        }
        status = BookingStatus.COMPLETED;
        updatedAt = Instant.now();
    }

    private void requireStatus(BookingStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    public Long getId() {
        return id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public BigDecimal getEstimatedDistanceKm() {
        return estimatedDistanceKm;
    }

    public long getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public BigDecimal getEstimatedFare() {
        return estimatedFare;
    }

    public String getFareCurrency() {
        return fareCurrency;
    }

    public VehicleCategory getVehicleCategory() {
        return vehicleCategory;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getAssignedDriverReference() {
        return assignedDriverReference;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public double getPickupLatitude() {
        return pickupLatitude;
    }

    public double getPickupLongitude() {
        return pickupLongitude;
    }

    public double getDestinationLatitude() {
        return destinationLatitude;
    }

    public double getDestinationLongitude() {
        return destinationLongitude;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public boolean isPeakHour() {
        return peakHour;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Trip getTrip() {
        return trip;
    }

    void setTrip(Trip trip) {
        this.trip = trip;
    }
}
