package com.serendib.taxibooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "receipts")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_reference", nullable = false, unique = true, length = 45)
    private String receiptReference;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Column(name = "pickup_address", nullable = false, length = 255)
    private String pickupAddress;

    @Column(name = "destination_address", nullable = false, length = 255)
    private String destinationAddress;

    @Column(name = "distance_km", nullable = false, precision = 10, scale = 3)
    private BigDecimal distanceKm;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    protected Receipt() {
    }

    public Receipt(String receiptReference, Payment payment) {
        this.receiptReference = receiptReference;
        this.payment = payment;
        this.pickupAddress = payment.getBooking().getPickupAddress();
        this.destinationAddress = payment.getBooking().getDestinationAddress();
        this.distanceKm = payment.getBooking().getEstimatedDistanceKm();
        this.issuedAt = Instant.now();
    }

    public String getReceiptReference() {
        return receiptReference;
    }

    public Payment getPayment() {
        return payment;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
