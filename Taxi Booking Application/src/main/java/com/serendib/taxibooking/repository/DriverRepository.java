package com.serendib.taxibooking.repository;

import com.serendib.taxibooking.entity.Driver;
import com.serendib.taxibooking.entity.DriverApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Driver d where d.id = :id")
    Optional<Driver> findByIdForUpdate(@Param("id") Long id);

    List<Driver> findAllByOrderByCreatedAtDesc();

    @Query("""
            select d from Driver d
            where (:status is null or d.applicationStatus = :status)
              and (:search is null or
                   lower(d.name) like lower(concat('%', :search, '%')) or
                   lower(d.email) like lower(concat('%', :search, '%')) or
                   lower(d.phone) like lower(concat('%', :search, '%')) or
                   lower(d.nicNumber) like lower(concat('%', :search, '%')) or
                   lower(d.licenseNumber) like lower(concat('%', :search, '%')) or
                   exists (select v.id from Vehicle v where v.driver = d and
                           lower(v.plateNumber) like lower(concat('%', :search, '%'))))
            order by d.createdAt desc
            """)
    List<Driver> searchApplications(@Param("status") DriverApplicationStatus status,
                                    @Param("search") String search);

    List<Driver> findAllByApprovedTrueOrderByNameAsc();

    Optional<Driver> findByEmailIgnoreCase(String email);

    Optional<Driver> findByDriverReference(String driverReference);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    boolean existsByLicenseNumberIgnoreCaseAndIdNot(String licenseNumber, Long id);

    boolean existsByNicNumberIgnoreCase(String nicNumber);

    boolean existsByNicNumberIgnoreCaseAndIdNot(String nicNumber, Long id);

    long countByApprovedFalse();

    long countByApplicationStatus(DriverApplicationStatus applicationStatus);

    long countByApprovedTrue();

    long countByApprovedTrueAndOnlineTrue();
}
