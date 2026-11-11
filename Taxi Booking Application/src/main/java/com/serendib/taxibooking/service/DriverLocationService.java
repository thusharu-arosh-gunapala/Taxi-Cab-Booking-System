package com.serendib.taxibooking.service;

import com.serendib.taxibooking.dto.DriverLocationUpdateRequest;
import com.serendib.taxibooking.dto.NearbyDriverResponse;
import com.serendib.taxibooking.entity.Driver;
import com.serendib.taxibooking.geo.HaversineDistanceCalculator;
import com.serendib.taxibooking.model.VehicleCategory;
import com.serendib.taxibooking.repository.DriverRepository;
import com.serendib.taxibooking.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class DriverLocationService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final double nearbyRadiusKm;
    private final Duration maxLocationAge;
    private final ConcurrentMap<Long, DriverLocation> liveLocations = new ConcurrentHashMap<>();

    public DriverLocationService(DriverRepository driverRepository,
                                 VehicleRepository vehicleRepository,
                                 @Value("${app.maps.nearby-driver-radius-km:5}") double nearbyRadiusKm,
                                 @Value("${app.maps.driver-location-max-age-seconds:120}") long maxLocationAgeSeconds) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        if (!Double.isFinite(nearbyRadiusKm) || nearbyRadiusKm <= 0 || nearbyRadiusKm > 50) {
            throw new IllegalArgumentException("Nearby driver radius must be greater than 0 and no more than 50 km.");
        }
        if (maxLocationAgeSeconds <= 0 || maxLocationAgeSeconds > 3600) {
            throw new IllegalArgumentException("Driver location age must be between 1 and 3600 seconds.");
        }
        this.nearbyRadiusKm = nearbyRadiusKm;
        this.maxLocationAge = Duration.ofSeconds(maxLocationAgeSeconds);
    }

    @Transactional(readOnly = true)
    public void updateLocation(Long driverId, DriverLocationUpdateRequest request) {
        if (request == null || request.latitude() == null || request.longitude() == null
                || !Double.isFinite(request.latitude()) || request.latitude() < -90 || request.latitude() > 90
                || !Double.isFinite(request.longitude()) || request.longitude() < -180
                || request.longitude() > 180) {
            throw new IllegalArgumentException("Valid driver latitude and longitude are required.");
        }
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver was not found."));
        if (!driver.isApproved() || !driver.isOnline()) {
            throw new IllegalStateException("Only approved, online drivers can publish a live location.");
        }
        liveLocations.put(driverId, new DriverLocation(
                request.latitude(), request.longitude(), Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<NearbyDriverResponse> findNearby(double pickupLatitude,
                                                 double pickupLongitude,
                                                 VehicleCategory category) {
        validateCoordinates(pickupLatitude, pickupLongitude);
        Instant cutoff = Instant.now().minus(maxLocationAge);
        liveLocations.entrySet().removeIf(entry -> entry.getValue().updatedAt().isBefore(cutoff));

        List<NearbyDriverResponse> nearby = new ArrayList<>();
        for (var vehicle : vehicleRepository
                .findAvailableApprovedByCategory(category)) {
            Driver driver = vehicle.getDriver();
            if (driver == null) {
                continue;
            }
            DriverLocation location = liveLocations.get(driver.getId());
            if (location == null || location.updatedAt().isBefore(cutoff)) {
                continue;
            }
            double distanceKm = HaversineDistanceCalculator.calculateKilometres(
                    pickupLatitude, pickupLongitude, location.latitude(), location.longitude());
            if (distanceKm <= nearbyRadiusKm) {
                nearby.add(new NearbyDriverResponse(
                        vehicle.getCategory(),
                        location.latitude(),
                        location.longitude(),
                        Math.round(distanceKm * 1000)));
            }
        }
        return nearby.stream()
                .sorted(Comparator.comparingLong(NearbyDriverResponse::distanceMetres))
                .limit(20)
                .toList();
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Valid pickup latitude and longitude are required.");
        }
    }

    private record DriverLocation(double latitude, double longitude, Instant updatedAt) {
    }
}
