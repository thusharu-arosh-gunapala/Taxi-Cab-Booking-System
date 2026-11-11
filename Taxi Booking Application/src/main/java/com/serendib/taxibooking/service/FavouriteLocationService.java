package com.serendib.taxibooking.service;

import com.serendib.taxibooking.dto.FavouriteLocationForm;
import com.serendib.taxibooking.entity.FavouriteLocation;
import com.serendib.taxibooking.entity.Passenger;
import com.serendib.taxibooking.repository.FavouriteLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavouriteLocationService {

    private final FavouriteLocationRepository favouriteLocationRepository;

    public FavouriteLocationService(FavouriteLocationRepository favouriteLocationRepository) {
        this.favouriteLocationRepository = favouriteLocationRepository;
    }

    @Transactional(readOnly = true)
    public List<FavouriteLocation> findForPassenger(Passenger passenger) {
        return favouriteLocationRepository.findAllByPassengerIdOrderByTypeAscIdAsc(passenger.getId());
    }

    @Transactional
    public FavouriteLocation add(Passenger passenger, FavouriteLocationForm form) {
        if ((form.getType() == FavouriteLocation.Type.HOME || form.getType() == FavouriteLocation.Type.WORK)
                && favouriteLocationRepository.existsByPassenger_IdAndType(passenger.getId(), form.getType())) {
            throw new IllegalArgumentException(
                    "You already have a " + form.getType() + " location. Delete it before adding another.");
        }
        return favouriteLocationRepository.save(new FavouriteLocation(
                passenger,
                form.getType(),
                form.getAddress().trim(),
                form.getLatitude(),
                form.getLongitude()));
    }

    @Transactional
    public void delete(Long id, Passenger passenger) {
        FavouriteLocation location = favouriteLocationRepository.findByIdAndPassengerId(id, passenger.getId())
                .orElseThrow(() -> new IllegalArgumentException("Favourite location was not found."));
        favouriteLocationRepository.delete(location);
    }
}
