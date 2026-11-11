package com.serendib.taxibooking.dto;

import com.serendib.taxibooking.entity.BookingStatus;

import java.time.Instant;

public record BookingStatusEvent(String bookingReference, BookingStatus status, Instant updatedAt) {
}
