package com.taxicab.model;

import java.time.LocalDate;

public class Review {
    private String reviewId;
    private String passengerId;
    private String driverId;
    private String bookingId;
    private int rating;          // 1 - 5
    private String comment;
    private LocalDate createdDate;

    public Review() { }

    public Review(String reviewId, String passengerId, String driverId, String bookingId,
                  int rating, String comment, LocalDate createdDate) {
        this.reviewId = reviewId;
        this.passengerId = passengerId;
        this.driverId = driverId;
        this.bookingId = bookingId;
        this.rating = rating;
        this.comment = comment;
        this.createdDate = createdDate;
    }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

    /** One line in reviews.txt: id|passenger|driver|booking|rating|comment|date */
    public String toFileLine() {
        String safeComment = comment == null ? "" : comment.replace("|", "/").replace("\n", " ").replace("\r", " ");
        return String.join("|", reviewId, passengerId, driverId, bookingId,
                String.valueOf(rating), safeComment, String.valueOf(createdDate));
    }

    public static Review fromFileLine(String line) {
        String[] p = line.split("\\|", -1);
        return new Review(p[0], p[1], p[2], p[3], Integer.parseInt(p[4]), p[5], LocalDate.parse(p[6]));
    }
}