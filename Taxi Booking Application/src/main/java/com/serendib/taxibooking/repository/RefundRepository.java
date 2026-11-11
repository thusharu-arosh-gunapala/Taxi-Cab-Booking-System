package com.serendib.taxibooking.repository;

import com.serendib.taxibooking.entity.Refund;
import com.serendib.taxibooking.entity.RefundStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    @EntityGraph(attributePaths = {"payment", "payment.booking"})
    List<Refund> findAllByPassengerIdOrderByRequestedAtDesc(Long passengerId);

    @EntityGraph(attributePaths = {"payment", "payment.booking", "passenger"})
    List<Refund> findAllByOrderByRequestedAtDesc();

    long countByStatus(RefundStatus status);

    boolean existsByPaymentIdAndStatus(Long paymentId, com.serendib.taxibooking.entity.RefundStatus status);
}
