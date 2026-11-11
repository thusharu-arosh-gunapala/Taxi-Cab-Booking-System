package com.serendib.taxibooking.entity;

public enum BookingStatus {
    PENDING,
    SEARCHING_FOR_DRIVER,
    DRIVER_ASSIGNED,
    ACCEPTED,
    ARRIVING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    REJECTED,
    NO_DRIVER_AVAILABLE
}
