package com.serendib.taxibooking.service;

import com.serendib.taxibooking.dto.BookingStatusEvent;
import com.serendib.taxibooking.entity.Booking;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class BookingEventPublisher {

    private static final long CONNECTION_TIMEOUT_MILLIS = 30L * 60L * 1000L;
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> subscriptions =
            new ConcurrentHashMap<>();

    public SseEmitter subscribe(Booking booking) {
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT_MILLIS);
        CopyOnWriteArrayList<SseEmitter> bookingSubscribers =
                subscriptions.computeIfAbsent(booking.getId(), ignored -> new CopyOnWriteArrayList<>());
        bookingSubscribers.add(emitter);
        emitter.onCompletion(() -> remove(booking.getId(), emitter));
        emitter.onTimeout(() -> {
            remove(booking.getId(), emitter);
            emitter.complete();
        });
        emitter.onError(error -> remove(booking.getId(), emitter));
        send(booking.getId(), emitter, new BookingStatusEvent(
                booking.getBookingReference(), booking.getStatus(), booking.getUpdatedAt()));
        return emitter;
    }

    public void publish(Booking booking) {
        List<SseEmitter> bookingSubscribers = subscriptions.get(booking.getId());
        if (bookingSubscribers == null) {
            return;
        }
        BookingStatusEvent event = new BookingStatusEvent(
                booking.getBookingReference(), booking.getStatus(), booking.getUpdatedAt());
        bookingSubscribers.forEach(emitter -> send(booking.getId(), emitter, event));
    }

    private void send(Long bookingId, SseEmitter emitter, BookingStatusEvent event) {
        try {
            emitter.send(SseEmitter.event().name("booking-status").data(event));
        } catch (IOException exception) {
            remove(bookingId, emitter);
            emitter.completeWithError(exception);
        }
    }

    private void remove(Long bookingId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> bookingSubscribers = subscriptions.get(bookingId);
        if (bookingSubscribers != null) {
            bookingSubscribers.remove(emitter);
            if (bookingSubscribers.isEmpty()) {
                subscriptions.remove(bookingId, bookingSubscribers);
            }
        }
    }
}
