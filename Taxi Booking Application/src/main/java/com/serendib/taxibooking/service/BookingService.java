package com.serendib.taxibooking.service;

import com.serendib.taxibooking.dto.BookingForm;
import com.serendib.taxibooking.dto.FareEstimateRequest;
import com.serendib.taxibooking.dto.FareEstimateResponse;
import com.serendib.taxibooking.entity.Booking;
import com.serendib.taxibooking.entity.Passenger;
import com.serendib.taxibooking.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FareEstimateService fareEstimateService;

    public BookingService(BookingRepository bookingRepository, FareEstimateService fareEstimateService) {
        this.bookingRepository = bookingRepository;
        this.fareEstimateService = fareEstimateService;
    }

    @Transactional
    public Booking create(Passenger passenger, BookingForm form) {
        FareEstimateResponse estimate = fareEstimateService.estimate(new FareEstimateRequest(
                form.getPickupLatitude(),
                form.getPickupLongitude(),
                form.getDestinationLatitude(),
                form.getDestinationLongitude(),
                form.getVehicleCategory(),
                form.isPeakHour()));

        Booking booking = new Booking(
                "RIDE-" + UUID.randomUUID(),
                passenger,
                form.getPickupAddress().trim(),
                form.getDestinationAddress().trim(),
                form.getPickupLatitude(),
                form.getPickupLongitude(),
                form.getDestinationLatitude(),
                form.getDestinationLongitude(),
                estimate.distanceKilometres(),
                estimate.estimatedFare(),
                estimate.durationMinutes(),
                estimate.currency(),
                form.getVehicleCategory(),
                form.isPeakHour(),
                form.getBookingTime() == null ? LocalDateTime.now() : form.getBookingTime());
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking update(Long bookingId, Passenger passenger, BookingForm form) {
        Booking booking = findOwnedBooking(bookingId, passenger);
        FareEstimateResponse estimate = estimate(form);
        booking.update(
                form.getPickupAddress().trim(),
                form.getDestinationAddress().trim(),
                form.getPickupLatitude(),
                form.getPickupLongitude(),
                form.getDestinationLatitude(),
                form.getDestinationLongitude(),
                estimate.distanceKilometres(),
                estimate.durationMinutes(),
                estimate.estimatedFare(),
                estimate.currency(),
                form.getVehicleCategory(),
                form.isPeakHour(),
                form.getBookingTime() == null ? LocalDateTime.now() : form.getBookingTime());
        return booking;
    }

    @Transactional(readOnly = true)
    public List<Booking> findForPassenger(Passenger passenger) {
        return bookingRepository.findAllByPassengerIdOrderByCreatedAtDesc(passenger.getId());
    }

    @Transactional
    public Booking findOwnedBooking(Long bookingId, Passenger passenger) {
        return bookingRepository.findByIdAndPassengerId(bookingId, passenger.getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking was not found."));
    }

    @Transactional
    public void cancel(Long bookingId, Passenger passenger, String reason) {
        findOwnedBooking(bookingId, passenger).cancel(reason);
    }

    private FareEstimateResponse estimate(BookingForm form) {
        return fareEstimateService.estimate(new FareEstimateRequest(
                form.getPickupLatitude(),
                form.getPickupLongitude(),
                form.getDestinationLatitude(),
                form.getDestinationLongitude(),
                form.getVehicleCategory(),
                form.isPeakHour()));
    }
}
