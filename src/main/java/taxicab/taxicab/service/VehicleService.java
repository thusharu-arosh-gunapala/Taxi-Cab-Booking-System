package taxicab.taxicab.service;

import taxicab.taxicab.model.*;
import taxicab.taxicab.repository.DriverRepository;
import taxicab.taxicab.repository.VehicleRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public VehicleService(
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Vehicle not found"));
    }

    public Vehicle createVehicle(
            String category,
            String vehicleNumber,
            String brand,
            String vehicleModel,
            int seats,
            Long driverId) {

        Vehicle vehicle;

        switch (category.toUpperCase().replace(" ", "_")) {

            case "TUK_TUK":
                vehicle = new TukTuk();
                break;

            case "MINI":
                vehicle = new Mini();
                break;

            case "CAR":
                vehicle = new Car();
                break;

            case "VAN":
                vehicle = new Van();
                break;

            case "PREMIUM":
                vehicle = new Premium();
                break;

            default:
                throw new IllegalArgumentException(
                        "Invalid vehicle category");
        }

        vehicle.setVehicleNumber(vehicleNumber);
        vehicle.setBrand(brand);
        vehicle.setModel(vehicleModel);
        vehicle.setSeats(seats);
        vehicle.setAvailable(true);

        setDriver(vehicle, driverId);

        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(
            Long id,
            Vehicle updatedVehicle,
            Long driverId) {

        Vehicle existing = getVehicleById(id);

        existing.setVehicleNumber(
                updatedVehicle.getVehicleNumber());

        existing.setBrand(updatedVehicle.getBrand());
        existing.setModel(updatedVehicle.getModel());
        existing.setSeats(updatedVehicle.getSeats());
        existing.setAvailable(updatedVehicle.isAvailable());

        setDriver(existing, driverId);

        return vehicleRepository.save(existing);
    }

    public void deleteVehicle(Long id) {
        vehicleRepository.delete(getVehicleById(id));
    }

    private void setDriver(Vehicle vehicle, Long driverId) {

        if (driverId == null) {
            vehicle.setDriver(null);
        } else {
            Driver driver = driverRepository.findById(driverId)
                    .orElseThrow(() ->
                            new RuntimeException("Driver not found"));

            vehicle.setDriver(driver);
        }
    }
}
