package com.serendib.taxibooking.repository;

import com.serendib.taxibooking.entity.Payment;
import com.serendib.taxibooking.entity.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"booking", "promotion", "receipt"})
    List<Payment> findAllByPassengerIdOrderByCreatedAtDesc(Long passengerId);

    @EntityGraph(attributePaths = {"booking", "passenger", "promotion", "receipt"})
    List<Payment> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"booking", "promotion", "receipt"})
    Optional<Payment> findByPaymentReferenceAndPassengerId(String paymentReference, Long passengerId);

    Optional<Payment> findByProviderSessionId(String providerSessionId);

    Optional<Payment> findByPaymentReference(String paymentReference);

    @EntityGraph(attributePaths = {"booking", "promotion", "receipt"})
    Optional<Payment> findByBookingIdAndPassengerId(Long bookingId, Long passengerId);

    boolean existsByBookingIdAndPassengerIdAndStatusIn(Long bookingId, Long passengerId,
                                                        List<PaymentStatus> statuses);
}
