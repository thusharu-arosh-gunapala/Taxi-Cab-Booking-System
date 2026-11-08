package taxicab.taxicab.repository;

import taxicab.taxicab.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehicleRepository
        extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByDriverId(Long driverId);
}
