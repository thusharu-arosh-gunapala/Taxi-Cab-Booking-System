package com.taxibooking.payment.model;

import java.text.SimpleDateFormat;
import java.util.Date;

// Refund request: starts as REQUESTED, then becomes REFUNDED or REJECTED
public class Refund {

    private String refundId;
    private String paymentId;
    private String bookingId;
    private double refundAmount;
    private String reason;
    private String refundStatus;
    private String requestDate;
    private String processedDate;

    public Refund(String refundId, String paymentId, String bookingId, double refundAmount, String reason) {
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.refundAmount = refundAmount;
        this.reason = reason;
        this.refundStatus = "REQUESTED";
        this.requestDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.processedDate = "Pending Review";
    }

    public String getRefundId() {
        return refundId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public String getReason() {
        return reason;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    public String getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(String processedDate) {
        this.processedDate = processedDate;
    }
}
