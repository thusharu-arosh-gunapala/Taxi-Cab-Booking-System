package com.serendib.taxibooking.service;

import com.serendib.taxibooking.entity.Booking;
import com.serendib.taxibooking.entity.BookingStatus;
import com.serendib.taxibooking.entity.Passenger;
import com.serendib.taxibooking.entity.Trip;
import com.serendib.taxibooking.repository.BookingRepository;
import com.serendib.taxibooking.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripService {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;

    public TripService(BookingRepository bookingRepository, TripRepository tripRepository) {
        this.bookingRepository = bookingRepository;
        this.tripRepository = tripRepository;
    }

    @Transactional(readOnly = true)
    public List<Trip> findForPassenger(Passenger passenger) {
        return tripRepository.findAllByBookingPassengerIdOrderByStartedAtDesc(passenger.getId());
    }

    @Transactional
    public Trip start(Long bookingId, Passenger passenger) {
        Booking booking = bookingRepository.findByIdAndPassengerId(bookingId, passenger.getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking was not found."));
        if (booking.getStatus() != BookingStatus.ACCEPTED && booking.getStatus() != BookingStatus.ARRIVING) {
            throw new IllegalStateException("A driver must accept the booking before a trip can start.");
        }
        booking.startTrip();
        return tripRepository.save(new Trip(booking));
    }

    @Transactional
    public Trip complete(Long bookingId, Passenger passenger) {
        Booking booking = bookingRepository.findByIdAndPassengerId(bookingId, passenger.getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking was not found."));
        if (booking.getStatus() != BookingStatus.IN_PROGRESS || booking.getTrip() == null) {
            throw new IllegalStateException("The trip must be in progress before it can be completed.");
        }
        booking.getTrip().complete();
        booking.completeTrip();
        return booking.getTrip();
    }
}
