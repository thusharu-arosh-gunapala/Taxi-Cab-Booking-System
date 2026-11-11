package com.serendib.taxibooking.repository;

import com.serendib.taxibooking.entity.Vehicle;
import com.serendib.taxibooking.model.VehicleCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    @EntityGraph(attributePaths = "driver")
    List<Vehicle> findAllByOrderByIdDesc();

    List<Vehicle> findAllByDriverId(Long driverId);

    boolean existsByDriverId(Long driverId);

    boolean existsByPlateNumberIgnoreCase(String plateNumber);

    boolean existsByPlateNumberIgnoreCaseAndIdNot(String plateNumber, Long id);

    long countByAvailableTrue();

    @Query("""
            select v from Vehicle v join fetch v.driver d
            where v.category = :category and v.available = true
              and d.approved = true and d.online = true
              and d.accountActive = true and d.suspended = false
              and d.applicationStatus = com.serendib.taxibooking.entity.DriverApplicationStatus.APPROVED
            """)
    List<Vehicle> findAvailableApprovedByCategory(@Param("category") VehicleCategory category);
}
