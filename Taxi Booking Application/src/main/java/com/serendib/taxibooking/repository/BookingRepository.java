package com.serendib.taxibooking.repository;

import com.serendib.taxibooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @EntityGraph(attributePaths = "passenger")
    List<Booking> findAllByOrderByCreatedAtDesc();
    List<Booking> findAllByPassengerIdOrderByCreatedAtDesc(Long passengerId);
    Optional<Booking> findByIdAndPassengerId(Long id, Long passengerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "passenger")
    List<Booking> findAllByAssignedDriverReferenceOrderByCreatedAtDesc(String assignedDriverReference);

    boolean existsByAssignedDriverReferenceAndStatusIn(String assignedDriverReference,
                                                       List<com.serendib.taxibooking.entity.BookingStatus> statuses);
}
