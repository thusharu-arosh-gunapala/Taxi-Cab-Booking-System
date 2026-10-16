package com.taxicab.dto.booking;

public class BookingStatusResponse {
    private Long bookingId;
    private String status;
    private String message;
    private Long driverId;

    public BookingStatusResponse() {
    }

    public BookingStatusResponse(Long bookingId, String status, String message, Long driverId) {
        this.bookingId = bookingId;
        this.status = status;
        this.message = message;
        this.driverId = driverId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }
}
