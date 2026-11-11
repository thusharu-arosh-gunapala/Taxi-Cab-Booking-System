package com.serendib.taxibooking.service;

import com.serendib.taxibooking.dto.VehicleForm;
import com.serendib.taxibooking.entity.Driver;
import com.serendib.taxibooking.entity.Vehicle;
import com.serendib.taxibooking.model.VehicleCategory;
import com.serendib.taxibooking.repository.DriverRepository;
import com.serendib.taxibooking.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public VehicleService(VehicleRepository vehicleRepository, DriverRepository driverRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findAll() {
        return vehicleRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findByDriverId(Long driverId) {
        return vehicleRepository.findAllByDriverId(driverId);
    }

    @Transactional(readOnly = true)
    public List<Driver> findEligibleDrivers() {
        return driverRepository.findAllByApprovedTrueOrderByNameAsc();
    }

    @Transactional
    public Vehicle findById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle was not found."));
    }

    @Transactional
    public Vehicle save(VehicleForm form) {
        String plate = normalizePlate(form.getPlateNumber());
        if (vehicleRepository.existsByPlateNumberIgnoreCase(plate)) {
            throw new IllegalArgumentException("A vehicle already uses this registration plate.");
        }
        return vehicleRepository.save(new Vehicle(
                form.getCategory(),
                form.getModel().trim(),
                plate,
                form.getColor().trim(),
                findEligibleDriver(form.getDriverId())));
    }

    @Transactional
    public Vehicle update(Long id, VehicleForm form) {
        Vehicle vehicle = findById(id);
        String plate = normalizePlate(form.getPlateNumber());
        if (vehicleRepository.existsByPlateNumberIgnoreCaseAndIdNot(plate, id)) {
            throw new IllegalArgumentException("A vehicle already uses this registration plate.");
        }
        vehicle.update(
                form.getCategory(),
                form.getModel().trim(),
                plate,
                form.getColor().trim(),
                findEligibleDriver(form.getDriverId()));
        return vehicle;
    }

    @Transactional
    public void delete(Long id) {
        vehicleRepository.delete(findById(id));
    }

    @Transactional
    public void toggleAvailability(Long id) {
        findById(id).toggleAvailability();
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findAvailableForCategory(VehicleCategory category) {
        return vehicleRepository.findAvailableApprovedByCategory(category);
    }

    @Transactional(readOnly = true)
    public long countAvailable() {
        return vehicleRepository.countByAvailableTrue();
    }

    private Driver findEligibleDriver(Long driverId) {
        if (driverId == null) {
            return null;
        }
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Selected driver was not found."));
        if (!driver.isApproved()) {
            throw new IllegalArgumentException("Vehicles can only be assigned to approved drivers.");
        }
        return driver;
    }

    private String normalizePlate(String plateNumber) {
        return plateNumber.trim().toUpperCase();
    }
}
