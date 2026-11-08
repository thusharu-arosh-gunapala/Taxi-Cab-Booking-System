package taxicab.taxicab.service;

import taxicab.taxicab.model.Driver;
import taxicab.taxicab.model.Vehicle;
import taxicab.taxicab.repository.DriverRepository;
import taxicab.taxicab.repository.VehicleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public DriverService(
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    public Driver getDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Driver not found"));
    }

    public Driver saveDriver(Driver driver) {
        return driverRepository.save(driver);
    }

    public Driver updateDriver(Long id, Driver updatedDriver) {

        Driver existing = getDriverById(id);

        existing.setFullName(updatedDriver.getFullName());
        existing.setEmail(updatedDriver.getEmail());
        existing.setPhone(updatedDriver.getPhone());
        existing.setLicenseNumber(
                updatedDriver.getLicenseNumber());

        return driverRepository.save(existing);
    }

    @Transactional
    public void deleteDriver(Long id) {

        Driver driver = getDriverById(id);

        List<Vehicle> vehicles =
                vehicleRepository.findByDriverId(id);

        for (Vehicle vehicle : vehicles) {
            vehicle.setDriver(null);
        }

        vehicleRepository.saveAll(vehicles);
        driverRepository.delete(driver);
    }
}
