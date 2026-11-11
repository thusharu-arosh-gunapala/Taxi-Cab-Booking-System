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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 45)
    private String paymentReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code")
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 24)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentStatus status;

    @Column(name = "original_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "provider_session_id", unique = true, length = 255)
    private String providerSessionId;

    @Column(name = "provider_transaction_reference", length = 255)
    private String providerTransactionReference;

    @Column(name = "promotion_usage_reserved", nullable = false)
    private boolean promotionUsageReserved;

    @Column(name = "checkout_attempt", nullable = false)
    private int checkoutAttempt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToOne(mappedBy = "payment", fetch = FetchType.LAZY)
    private Receipt receipt;

    protected Payment() {
    }

    public Payment(String paymentReference, Booking booking, Passenger passenger, Promotion promotion,
                   PaymentMethod method, BigDecimal originalAmount, BigDecimal discountAmount,
                   BigDecimal amount, String currency) {
        this.paymentReference = paymentReference;
        this.booking = booking;
        this.passenger = passenger;
        this.promotion = promotion;
        this.method = method;
        this.status = PaymentStatus.PENDING;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.amount = amount;
        this.currency = currency;
        this.promotionUsageReserved = promotion != null;
        this.checkoutAttempt = 1;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void attachProviderSession(String sessionId) {
        if (sessionId != null && sessionId.equals(providerSessionId)) {
            return;
        }
        requirePending();
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("The payment provider did not return a session reference.");
        }
        providerSessionId = sessionId;
        updatedAt = Instant.now();
    }

    public void markSucceeded(String transactionReference) {
        if (status == PaymentStatus.SUCCEEDED) {
            return;
        }
        requirePending();
        status = PaymentStatus.SUCCEEDED;
        providerTransactionReference = transactionReference;
        consumePromotionReservation();
        updatedAt = Instant.now();
    }

    public void markCashCollected(String transactionReference) {
        if (method != PaymentMethod.CASH) {
            throw new IllegalStateException("Only a cash payment can be marked as collected.");
        }
        markSucceeded(transactionReference);
    }

    public void markFailed() {
        requirePending();
        status = PaymentStatus.FAILED;
        releasePromotionReservation();
        updatedAt = Instant.now();
    }

    public void retry(Promotion promotion, PaymentMethod method, BigDecimal discountAmount,
                      BigDecimal amount) {
        if (status != PaymentStatus.FAILED) {
            throw new IllegalStateException("Only a failed payment can be retried.");
        }
        this.promotion = promotion;
        this.method = method;
        this.discountAmount = discountAmount;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.promotionUsageReserved = promotion != null;
        this.checkoutAttempt++;
        this.providerSessionId = null;
        this.providerTransactionReference = null;
        this.updatedAt = Instant.now();
    }

    public void requestRefund() {
        if (status != PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("Only settled payments can be refunded.");
        }
        status = PaymentStatus.REFUND_REQUESTED;
        updatedAt = Instant.now();
    }

    public void markRefunded() {
        if (status != PaymentStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("Only requested refunds can be completed.");
        }
        status = PaymentStatus.REFUNDED;
        updatedAt = Instant.now();
    }

    private void requirePending() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("This payment is no longer pending.");
        }
    }

    private void consumePromotionReservation() {
        if (promotionUsageReserved) {
            promotion.consumeReservation();
            promotionUsageReserved = false;
        }
    }

    private void releasePromotionReservation() {
        if (promotionUsageReserved) {
            promotion.releaseReservation();
            promotionUsageReserved = false;
        }
    }

    public Long getId() {
        return id;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public Booking getBooking() {
        return booking;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProviderSessionId() {
        return providerSessionId;
    }

    public String getProviderTransactionReference() {
        return providerTransactionReference;
    }

    public int getCheckoutAttempt() {
        return checkoutAttempt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Receipt getReceipt() {
        return receipt;
    }
}
