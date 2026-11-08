package taxicab.taxicab.repository;

import taxicab.taxicab.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository
        extends JpaRepository<Driver, Long> {
}
