package com.taxicab.controller;

import com.taxicab.model.Rating;
import com.taxicab.model.Review;
import com.taxicab.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // ---------- Pages ----------
    @GetMapping("/reviews")
    public String reviewsPage() {
        return "review/reviews";
    }

    // ---------- REST API (used by JavaScript fetch) ----------
    @GetMapping("/api/reviews")
    @ResponseBody
    public List<Review> getReviews(@RequestParam(required = false) String driverId) {
        return (driverId == null || driverId.isBlank())
                ? reviewService.getAllReviews()
                : reviewService.getReviewsByDriver(driverId);
    }

    @PostMapping("/api/reviews")
    @ResponseBody
    public ResponseEntity<Review> createReview(@RequestBody Review review) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.submitReview(review));
    }

    @PutMapping("/api/reviews/{id}")
    @ResponseBody
    public Review updateReview(@PathVariable String id, @RequestBody Review body) {
        return reviewService.updateReview(id, body.getRating(), body.getComment());
    }

    @DeleteMapping("/api/reviews/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteReview(@PathVariable String id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/drivers/{driverId}/rating")
    @ResponseBody
    public Rating getDriverRating(@PathVariable String driverId) {
        return reviewService.getDriverRating(driverId);
    }
}