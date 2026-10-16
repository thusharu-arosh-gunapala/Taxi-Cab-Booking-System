package com.taxicab.exception;

public class UnauthorizedBookingAccessException extends RuntimeException {
    public UnauthorizedBookingAccessException(Long bookingId) {
        super("Unauthorized access to booking: " + bookingId);
    }
}
