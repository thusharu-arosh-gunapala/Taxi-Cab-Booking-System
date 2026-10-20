package com.taxicab.model;

/** Summary of all reviews for one driver (calculated, not stored in a file). */
public class Rating {
    private final String driverId;
    private final double averageRating;
    private final int totalReviews;

    public Rating(String driverId, double averageRating, int totalReviews) {
        this.driverId = driverId;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
    }

    public String getDriverId() { return driverId; }
    public double getAverageRating() { return averageRating; }
    public int getTotalReviews() { return totalReviews; }
}