package com.serendib.taxibooking.repository;

import com.serendib.taxibooking.entity.Receipt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    @EntityGraph(attributePaths = {"payment", "payment.booking"})
    Optional<Receipt> findByReceiptReferenceAndPaymentPassengerId(String receiptReference, Long passengerId);

    Optional<Receipt> findByPaymentId(Long paymentId);
}
