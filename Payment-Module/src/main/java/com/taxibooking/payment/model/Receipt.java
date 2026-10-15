package com.taxibooking.payment.model;

import java.text.SimpleDateFormat;
import java.util.Date;

// Receipt for a ride (saved in receipts.txt)
public class Receipt {

    private String receiptId;
    private String bookingId;
    private String passengerId;
    private String driver;
    private String pickup;
    private String destination;
    private double distance;
    private double originalFare;
    private double discount;
    private double total;
    private String paymentMethod;
    private String status;        // PAID or REFUNDED
    private String issueDate;

    public Receipt(String receiptId, String bookingId, String passengerId, String driver, String pickup,
                   String destination, double distance, double originalFare, double discount,
                   double total, String paymentMethod, String status) {
        this.receiptId = receiptId;
        this.bookingId = bookingId;
        this.passengerId = passengerId;
        this.driver = driver;
        this.pickup = pickup;
        this.destination = destination;
        this.distance = distance;
        this.originalFare = originalFare;
        this.discount = discount;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.issueDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public String getDriver() {
        return driver;
    }

    public String getPickup() {
        return pickup;
    }

    public String getDestination() {
        return destination;
    }

    public double getDistance() {
        return distance;
    }

    public double getOriginalFare() {
        return originalFare;
    }

    public double getDiscount() {
        return discount;
    }

    public double getTotal() {
        return total;
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

    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        this.issueDate = issueDate;
    }
}
