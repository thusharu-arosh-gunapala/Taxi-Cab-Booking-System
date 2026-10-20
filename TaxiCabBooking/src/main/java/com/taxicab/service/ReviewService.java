package com.taxicab.service;

import com.taxicab.exception.ResourceNotFoundException;
import com.taxicab.exception.ValidationException;
import com.taxicab.model.Rating;
import com.taxicab.model.Review;
import com.taxicab.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository repository;

    public ReviewService(ReviewRepository repository) {
        this.repository = repository;
    }

    public Review submitReview(Review review) {
        requireText(review.getPassengerId(), "Passenger ID");
        requireText(review.getDriverId(), "Driver ID");
        requireText(review.getBookingId(), "Booking ID");
        validateRatingAndComment(review.getRating(), review.getComment());

        boolean alreadyReviewed = repository.findAll().stream()
                .anyMatch(r -> r.getBookingId().equals(review.getBookingId()));
        if (alreadyReviewed) {
            throw new ValidationException("This booking already has a review.");
        }

        review.setCreatedDate(LocalDate.now());
        return repository.save(review);
    }

    public List<Review> getAllReviews() {
        return repository.findAll();
    }

    public List<Review> getReviewsByDriver(String driverId) {
        return repository.findAll().stream()
                .filter(r -> r.getDriverId().equals(driverId))
                .toList();
    }

    /** Only the rating and comment can be edited. */
    public Review updateReview(String reviewId, int rating, String comment) {
        Review existing = repository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review " + reviewId + " not found."));
        validateRatingAndComment(rating, comment);
        existing.setRating(rating);
        existing.setComment(comment);
        repository.update(existing);
        return existing;
    }

    public void deleteReview(String reviewId) {
        if (!repository.deleteById(reviewId)) {
            throw new ResourceNotFoundException("Review " + reviewId + " not found.");
        }
    }

    /** Average of all ratings given to one driver, rounded to 1 decimal place. */
    public Rating getDriverRating(String driverId) {
        List<Review> reviews = getReviewsByDriver(driverId);
        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        double rounded = Math.round(average * 10.0) / 10.0;
        return new Rating(driverId, rounded, reviews.size());
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required.");
        }
    }

    private void validateRatingAndComment(int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new ValidationException("Rating must be between 1 and 5.");
        }
        if (comment != null && comment.length() > 300) {
            throw new ValidationException("Comment must be 300 characters or fewer.");
        }
    }
}