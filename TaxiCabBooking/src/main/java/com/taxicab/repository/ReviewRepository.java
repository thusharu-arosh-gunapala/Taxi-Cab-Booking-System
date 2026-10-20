package com.taxicab.repository;

import com.taxicab.model.Review;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ReviewRepository {

    private final Path file = Paths.get("data", "reviews.txt");

    public ReviewRepository() {
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot prepare reviews.txt", e);
        }
    }

    public synchronized List<Review> findAll() {
        List<Review> reviews = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file)) {
                if (!line.isBlank()) {
                    reviews.add(Review.fromFileLine(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read reviews.txt", e);
        }
        return reviews;
    }

    public synchronized Optional<Review> findById(String id) {
        return findAll().stream().filter(r -> r.getReviewId().equals(id)).findFirst();
    }

    /** Create: assigns the next ID (R001, R002 ...) and appends to the file. */
    public synchronized Review save(Review review) {
        List<Review> all = findAll();
        review.setReviewId(nextId(all));
        all.add(review);
        writeAll(all);
        return review;
    }

    /** Update: replaces the review that has the same ID. */
    public synchronized boolean update(Review updated) {
        List<Review> all = findAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getReviewId().equals(updated.getReviewId())) {
                all.set(i, updated);
                writeAll(all);
                return true;
            }
        }
        return false;
    }

    /** Delete: removes the review and rewrites the file. */
    public synchronized boolean deleteById(String id) {
        List<Review> all = findAll();
        boolean removed = all.removeIf(r -> r.getReviewId().equals(id));
        if (removed) {
            writeAll(all);
        }
        return removed;
    }

    private void writeAll(List<Review> reviews) {
        List<String> lines = new ArrayList<>();
        for (Review r : reviews) {
            lines.add(r.toFileLine());
        }
        try {
            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write reviews.txt", e);
        }
    }

    private String nextId(List<Review> reviews) {
        int max = 0;
        for (Review r : reviews) {
            max = Math.max(max, Integer.parseInt(r.getReviewId().substring(1)));
        }
        return String.format("R%03d", max + 1);
    }
}