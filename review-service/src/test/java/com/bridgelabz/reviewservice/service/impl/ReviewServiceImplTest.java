package com.bridgelabz.reviewservice.service.impl;

import com.bridgelabz.reviewservice.dto.ReviewRequest;
import com.bridgelabz.reviewservice.dto.ReviewResponse;
import com.bridgelabz.reviewservice.exception.DuplicateReviewException;
import com.bridgelabz.reviewservice.model.Review;
import com.bridgelabz.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private ReviewRequest reviewRequest;
    private Review review;

    @BeforeEach
    void setUp() {
        reviewRequest = ReviewRequest.builder()
                .appointmentId("APP-0092")
                .patientId("PAT-8802")
                .providerId("PROV-101")
                .rating(5)
                .comment("Dr. Amit was very professional and explained the diagnosis clearly.")
                .isAnonymous(false)
                .build();

        review = Review.builder()
                .reviewId("REV-505")
                .appointmentId("APP-0092")
                .rating(5)
                .isFlagged(false)
                .build();
    }

    @Test
    void addReview_Success() {
        when(reviewRepository.existsByAppointmentId("APP-0092")).thenReturn(false);
        when(reviewRepository.save(any())).thenReturn(review);
        ReviewResponse response = reviewService.addReview(reviewRequest);
        assertNotNull(response);
        verify(reviewRepository).save(any());
    }

    @Test
    void addReview_Duplicate() {
        when(reviewRepository.existsByAppointmentId("APP-0092")).thenReturn(true);
        assertThrows(DuplicateReviewException.class, () -> reviewService.addReview(reviewRequest));
    }

    @Test
    void getAvgRating_Success() {
        when(reviewRepository.avgRatingByProviderId("PROV-101")).thenReturn(4.8);
        Double avg = reviewService.getAvgRating("PROV-101");
        assertEquals(4.8, avg);
    }

    @Test
    void flagReview_Success() {
        when(reviewRepository.findById("REV-505")).thenReturn(Optional.of(review));
        when(reviewRepository.save(any())).thenReturn(review);
        ReviewResponse response = reviewService.flagReview("REV-505", "Inappropriate language used in comments");
        assertTrue(response.getIsFlagged());
        assertEquals("Inappropriate language used in comments", response.getFlagReason());
    }

    @Test
    void getByProvider_ReturnsList() {
        when(reviewRepository.findByProviderId("PROV-101")).thenReturn(Collections.singletonList(review));
        List<ReviewResponse> results = reviewService.getByProvider("PROV-101");
        assertFalse(results.isEmpty());
    }
}
