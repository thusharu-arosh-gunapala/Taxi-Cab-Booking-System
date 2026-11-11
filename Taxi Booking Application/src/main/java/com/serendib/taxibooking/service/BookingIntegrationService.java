package com.serendib.taxibooking.service;

import com.serendib.taxibooking.entity.Booking;
import com.serendib.taxibooking.entity.BookingStatus;
import com.serendib.taxibooking.entity.Trip;
import com.serendib.taxibooking.entity.Driver;
import com.serendib.taxibooking.repository.BookingRepository;
import com.serendib.taxibooking.repository.DriverRepository;
import com.serendib.taxibooking.repository.VehicleRepository;
import com.serendib.taxibooking.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookingIntegrationService {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final BookingEventPublisher eventPublisher;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public BookingIntegrationService(BookingRepository bookingRepository,
                                     TripRepository tripRepository,
                                     BookingEventPublisher eventPublisher,
                                     DriverRepository driverRepository,
                                     VehicleRepository vehicleRepository) {
        this.bookingRepository = bookingRepository;
        this.tripRepository = tripRepository;
        this.eventPublisher = eventPublisher;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public Booking beginSearch(Long bookingId) {
        Booking booking = findBooking(bookingId);
        booking.beginDriverSearch();
        return publish(booking);
    }

    @Transactional
    public Booking assignDriver(Long bookingId, String driverReference) {
        Booking booking = findBookingForUpdate(bookingId);
        Driver driver = eligibleDriver(driverReference);
        boolean categoryMatch = vehicleRepository.findAllByDriverId(driver.getId()).stream()
                .anyMatch(vehicle -> vehicle.isAvailable()
                        && vehicle.getCategory() == booking.getVehicleCategory());
        if (!categoryMatch) {
            throw new IllegalStateException("The selected driver has no available vehicle in the requested category.");
        }
        if (bookingRepository.existsByAssignedDriverReferenceAndStatusIn(driverReference, List.of(
                BookingStatus.DRIVER_ASSIGNED, BookingStatus.ACCEPTED, BookingStatus.ARRIVING,
                BookingStatus.IN_PROGRESS))) {
            throw new IllegalStateException("The selected driver already has an active ride assignment.");
        }
        booking.assignDriver(driverReference);
        return publish(booking);
    }

    @Transactional
    public Booking respondToAssignment(Long bookingId, boolean accepted) {
        Booking booking = findBookingForUpdate(bookingId);
        booking.respondToAssignment(accepted);
        return publish(booking);
    }

    @Transactional
    public Booking respondToAssignment(Long bookingId, String driverReference, boolean accepted) {
        Booking booking = findBookingForUpdate(bookingId);
        if (!driverReference.equals(booking.getAssignedDriverReference())) {
            throw new IllegalArgumentException("This ride offer is not assigned to your account.");
        }
        eligibleDriver(driverReference);
        if (accepted && bookingRepository.existsByAssignedDriverReferenceAndStatusIn(driverReference, List.of(
                BookingStatus.ACCEPTED, BookingStatus.ARRIVING, BookingStatus.IN_PROGRESS))) {
            throw new IllegalStateException("You already have an active ride.");
        }
        booking.respondToAssignment(accepted);
        return publish(booking);
    }

    @Transactional
    public Booking markNoDriverAvailable(Long bookingId) {
        Booking booking = findBooking(bookingId);
        booking.markNoDriverAvailable();
        return publish(booking);
    }

    @Transactional
    public Booking markDriverArriving(Long bookingId) {
        Booking booking = findBooking(bookingId);
        booking.markDriverArriving();
        return publish(booking);
    }

    @Transactional
    public Booking startTrip(Long bookingId) {
        Booking booking = findBooking(bookingId);
        booking.startTrip();
        tripRepository.save(new Trip(booking));
        return publish(booking);
    }

    @Transactional
    public Booking completeTrip(Long bookingId) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != BookingStatus.IN_PROGRESS || booking.getTrip() == null) {
            throw new IllegalStateException("The trip must be in progress before it can be completed.");
        }
        booking.getTrip().complete();
        booking.completeTrip();
        return publish(booking);
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking was not found."));
    }

    private Booking findBookingForUpdate(Long bookingId) {
        return bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking was not found."));
    }

    private Driver eligibleDriver(String driverReference) {
        Driver driver = driverRepository.findByDriverReference(driverReference)
                .orElseThrow(() -> new IllegalArgumentException("Driver was not found."));
        if (!driver.isApproved() || !driver.isAccountActive() || driver.isSuspended()
                || !driver.isOnline()
                || driver.getApplicationStatus() != com.serendib.taxibooking.entity.DriverApplicationStatus.APPROVED) {
            throw new IllegalStateException("Only approved, active, online drivers can receive or accept jobs.");
        }
        return driver;
    }

    private Booking publish(Booking booking) {
        eventPublisher.publish(booking);
        return booking;
    }
}
