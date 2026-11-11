package com.serendib.taxibooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_reference", nullable = false, unique = true, length = 45)
    private String refundReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private RefundStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    protected Refund() {
    }

    public Refund(String refundReference, Payment payment, Passenger passenger, String reason) {
        this.refundReference = refundReference;
        this.payment = payment;
        this.passenger = passenger;
        this.reason = reason;
        this.amount = payment.getAmount();
        this.status = RefundStatus.REQUESTED;
        this.requestedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getRefundReference() {
        return refundReference;
    }

    public Payment getPayment() {
        return payment;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public String getReason() {
        return reason;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }
}
