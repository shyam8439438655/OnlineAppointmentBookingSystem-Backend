package com.bridgelabz.reviewservice.resource;

import com.bridgelabz.reviewservice.dto.ReviewRequest;
import com.bridgelabz.reviewservice.dto.ReviewResponse;
import com.bridgelabz.reviewservice.dto.ReviewUpdateRequest;
import com.bridgelabz.reviewservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewResource {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(@Valid @RequestBody ReviewRequest request) {
        return new ResponseEntity<>(reviewService.addReview(request), HttpStatus.CREATED);
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ReviewResponse>> getByProvider(@PathVariable String providerId) {
        return ResponseEntity.ok(reviewService.getByProvider(providerId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<ReviewResponse>> getByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(reviewService.getByPatient(patientId));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<ReviewResponse> getByAppointment(@PathVariable String appointmentId) {
        return ResponseEntity.ok(reviewService.getByAppointment(appointmentId));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable String reviewId,
                                                       @Valid @RequestBody ReviewUpdateRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, request));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable String reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/provider/{providerId}/avg-rating")
    public ResponseEntity<Double> getAvgRating(@PathVariable String providerId) {
        return ResponseEntity.ok(reviewService.getAvgRating(providerId));
    }

    @GetMapping("/provider/{providerId}/count")
    public ResponseEntity<Long> getCount(@PathVariable String providerId) {
        return ResponseEntity.ok(reviewService.getReviewCount(providerId));
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @GetMapping("/flagged")
    public ResponseEntity<List<ReviewResponse>> getFlaggedReviews() {
        return ResponseEntity.ok(reviewService.getFlaggedReviews());
    }

    @PutMapping("/{reviewId}/flag")
    public ResponseEntity<ReviewResponse> flagReview(@PathVariable String reviewId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(reviewService.flagReview(reviewId, body.get("reason")));
    }

    @PutMapping("/{reviewId}/unflag")
    public ResponseEntity<ReviewResponse> unflagReview(@PathVariable String reviewId) {
        return ResponseEntity.ok(reviewService.unflagReview(reviewId));
    }

    @DeleteMapping("/deleteAll")
    public ResponseEntity<Void> deleteAll() {
        reviewService.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
