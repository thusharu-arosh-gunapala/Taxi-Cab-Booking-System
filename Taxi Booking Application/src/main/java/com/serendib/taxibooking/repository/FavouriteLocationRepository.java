package com.serendib.taxibooking.repository;

import com.serendib.taxibooking.entity.FavouriteLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavouriteLocationRepository extends JpaRepository<FavouriteLocation, Long> {
    List<FavouriteLocation> findAllByPassengerIdOrderByTypeAscIdAsc(Long passengerId);
    Optional<FavouriteLocation> findByIdAndPassengerId(Long id, Long passengerId);
    boolean existsByPassenger_IdAndType(Long passengerId, FavouriteLocation.Type type);
}
