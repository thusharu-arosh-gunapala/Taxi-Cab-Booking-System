package com.taxibooking.payment.model;

// A promo code: percent off, minimum fare, expiry date and active flag
public class Promotion {

    private String promoCode;
    private double discountPercentage;
    private double minimumFare;
    private String expiryDate;    // yyyy-MM-dd
    private boolean active;

    // used by the promotions form
    public Promotion() {
        this.active = true;
    }

    public Promotion(String promoCode, double discountPercentage, double minimumFare, String expiryDate, boolean active) {
        this.promoCode = promoCode;
        this.discountPercentage = discountPercentage;
        this.minimumFare = minimumFare;
        this.expiryDate = expiryDate;
        this.active = active;
    }

    public double calculateDiscount(double fare) {
        double discount = fare * discountPercentage / 100.0;
        if (discount > fare) {
            discount = fare;
        }
        return discount;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public double getMinimumFare() {
        return minimumFare;
    }

    public void setMinimumFare(double minimumFare) {
        this.minimumFare = minimumFare;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
