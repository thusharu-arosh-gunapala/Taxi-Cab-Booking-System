package com.taxicab.dto.booking;

public class DriverAssignmentResponse {
    private Long bookingId;
    private Long driverId;
    private String status;
    private boolean assigned;
    private String message;

    public DriverAssignmentResponse() {
    }

    public DriverAssignmentResponse(Long bookingId, Long driverId, String status, boolean assigned, String message) {
        this.bookingId = bookingId;
        this.driverId = driverId;
        this.status = status;
        this.assigned = assigned;
        this.message = message;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAssigned() {
        return assigned;
    }

    public void setAssigned(boolean assigned) {
        this.assigned = assigned;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
