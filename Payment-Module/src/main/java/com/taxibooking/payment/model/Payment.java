package com.taxibooking.payment.model;

import java.text.SimpleDateFormat;
import java.util.Date;

// Base class for all payments (CASH, CARD, ONLINE)
public abstract class Payment {

    private String paymentId;
    private String bookingId;
    private String passengerId;
    private double amount;
    private String paymentMethod;
    private String status;        // PENDING, PAID, FAILED or REFUNDED
    private String paymentDate;
    private String notes;

    public Payment(String paymentId, String bookingId, String passengerId, double amount, String paymentMethod) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.passengerId = passengerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = "PENDING";
        this.paymentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.notes = "Payment created";
    }

    // each payment type checks its own details and sets PAID or FAILED
    public abstract boolean processPayment();

    public String getPaymentId() {
        return paymentId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public double getAmount() {
        return amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // ---------- Cash ----------
    public static class CashPayment extends Payment {
        private double amountTendered;
        private double changeAmount;

        public CashPayment(String paymentId, String bookingId, String passengerId, double amount, double amountTendered) {
            super(paymentId, bookingId, passengerId, amount, "CASH");
            this.amountTendered = amountTendered;
        }

        public boolean processPayment() {
            if (amountTendered >= getAmount()) {
                changeAmount = amountTendered - getAmount();
                setStatus("PAID");
                setNotes(String.format("Cash received: $%.2f. Change given: $%.2f", amountTendered, changeAmount));
                return true;
            }
            setStatus("FAILED");
            setNotes(String.format("Payment Failed: Insufficient cash tendered. Short by $%.2f", getAmount() - amountTendered));
            return false;
        }

        public double getAmountTendered() {
            return amountTendered;
        }

        public double getChangeAmount() {
            return changeAmount;
        }

        public void setChangeAmount(double changeAmount) {
            this.changeAmount = changeAmount;
        }
    }

    // ---------- Card ----------
    public static class CardPayment extends Payment {
        private String cardNumber;
        private String cardHolderName;
        private String expiryDate;
        private String cvv;

        public CardPayment(String paymentId, String bookingId, String passengerId, double amount,
                           String cardNumber, String cardHolderName, String expiryDate, String cvv) {
            super(paymentId, bookingId, passengerId, amount, "CARD");
            this.cardNumber = cardNumber;
            this.cardHolderName = cardHolderName;
            this.expiryDate = expiryDate;
            this.cvv = cvv;
        }

        public boolean processPayment() {
            String number = "";
            if (cardNumber != null) {
                number = cardNumber.replace(" ", "").replace("-", "");
            }
            String code = cvv;

            // keep only the last 4 digits, never keep the CVV (even if the payment fails)
            if (number.length() >= 4) {
                cardNumber = "**** **** **** " + number.substring(number.length() - 4);
            } else {
                cardNumber = "****";
            }
            cvv = "";

            if (cardHolderName == null || cardHolderName.trim().isEmpty()) {
                setStatus("FAILED");
                setNotes("Payment Failed: Card holder name is required.");
                return false;
            }
            if (number.length() < 13 || number.length() > 19) {
                setStatus("FAILED");
                setNotes("Payment Failed: Card number must be 13 to 19 digits.");
                return false;
            }
            if (code == null || code.trim().length() < 3 || code.trim().length() > 4) {
                setStatus("FAILED");
                setNotes("Payment Failed: Invalid CVV code. Must be 3 or 4 digits.");
                return false;
            }
            if (expiryDate == null || expiryDate.trim().length() < 4) {
                setStatus("FAILED");
                setNotes("Payment Failed: Invalid card expiry date (use MM/YY).");
                return false;
            }

            setStatus("PAID");
            setNotes("Card authorized successfully via Gateway. Card: " + cardNumber);
            return true;
        }

        public String getCardNumber() {
            return cardNumber;
        }

        public String getCardHolderName() {
            return cardHolderName;
        }

        public String getExpiryDate() {
            return expiryDate;
        }
    }

    // ---------- Online ----------
    public static class OnlinePayment extends Payment {
        private String provider;
        private String transactionReference;

        public OnlinePayment(String paymentId, String bookingId, String passengerId, double amount,
                             String provider, String transactionReference) {
            super(paymentId, bookingId, passengerId, amount, "ONLINE");
            this.provider = provider;
            this.transactionReference = transactionReference;
        }

        public boolean processPayment() {
            if (provider == null || provider.trim().isEmpty()) {
                setStatus("FAILED");
                setNotes("Payment Failed: Please select a payment provider.");
                return false;
            }
            if (transactionReference == null || transactionReference.trim().isEmpty()) {
                setStatus("FAILED");
                setNotes("Payment Failed: Please enter transaction reference or UPI ID.");
                return false;
            }
            setStatus("PAID");
            setNotes(String.format("Digital payment settled via %s (Ref: %s)", provider, transactionReference));
            return true;
        }

        public String getProvider() {
            return provider;
        }

        public String getTransactionReference() {
            return transactionReference;
        }
    }
}
