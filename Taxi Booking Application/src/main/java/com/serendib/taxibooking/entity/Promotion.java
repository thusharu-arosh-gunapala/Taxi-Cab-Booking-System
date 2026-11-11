package com.serendib.taxibooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Entity
@Table(name = "promotions")
public class Promotion {

    @Id
    @Column(name = "promo_code", length = 32)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private PromotionDiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "minimum_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumFare;

    @Column(name = "maximum_discount", precision = 12, scale = 2)
    private BigDecimal maximumDiscount;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Column(name = "usage_limit")
    private Long usageLimit;

    @Column(name = "usage_count", nullable = false)
    private long usageCount;

    @Column(name = "reserved_count", nullable = false)
    private long reservedCount;

    @Column(nullable = false)
    private boolean active;

    @Version
    private long version;

    protected Promotion() {
    }

    public Promotion(String code, PromotionDiscountType discountType, BigDecimal discountValue,
                     BigDecimal minimumFare, BigDecimal maximumDiscount, LocalDate expiresOn,
                     Long usageLimit) {
        if (code == null || !code.matches("[A-Za-z0-9_-]{1,32}")) {
            throw new IllegalArgumentException("Promotion code must contain 1 to 32 letters, numbers, _ or -.");
        }
        if (discountType == null || discountValue == null || discountValue.signum() <= 0) {
            throw new IllegalArgumentException("Promotion discount must be positive.");
        }
        if (discountType == PromotionDiscountType.PERCENTAGE
                && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discounts cannot exceed 100.");
        }
        if (minimumFare == null || minimumFare.signum() < 0
                || maximumDiscount != null && maximumDiscount.signum() <= 0
                || usageLimit != null && usageLimit < 1) {
            throw new IllegalArgumentException("Promotion limits must be valid positive amounts.");
        }
        this.code = code.toUpperCase(Locale.ROOT);
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumFare = minimumFare;
        this.maximumDiscount = maximumDiscount;
        this.expiresOn = expiresOn;
        this.usageLimit = usageLimit;
        this.active = true;
    }

    public String getCode() {
        return code;
    }

    public PromotionDiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public BigDecimal getMinimumFare() {
        return minimumFare;
    }

    public BigDecimal getMaximumDiscount() {
        return maximumDiscount;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public Long getUsageLimit() {
        return usageLimit;
    }

    public long getUsageCount() {
        return usageCount;
    }

    public long getReservedCount() {
        return reservedCount;
    }

    public boolean isActive() {
        return active;
    }

    public BigDecimal calculateDiscount(BigDecimal fare) {
        BigDecimal discount = discountType == PromotionDiscountType.PERCENTAGE
                ? fare.multiply(discountValue).movePointLeft(2)
                : discountValue;
        if (maximumDiscount != null) {
            discount = discount.min(maximumDiscount);
        }
        return discount.min(fare).max(BigDecimal.ZERO);
    }

    public void reserveUsage() {
        if (usageLimit != null && usageCount + reservedCount >= usageLimit) {
            throw new IllegalStateException("This promotion has reached its usage limit.");
        }
        reservedCount++;
    }

    public void consumeReservation() {
        if (reservedCount < 1) {
            throw new IllegalStateException("Promotion reservation is missing.");
        }
        reservedCount--;
        usageCount++;
    }

    public void releaseReservation() {
        if (reservedCount > 0) {
            reservedCount--;
        }
    }

    public boolean isAvailable() {
        return active
                && (expiresOn == null || !expiresOn.isBefore(LocalDate.now()))
                && (usageLimit == null || usageCount + reservedCount < usageLimit);
    }
}
