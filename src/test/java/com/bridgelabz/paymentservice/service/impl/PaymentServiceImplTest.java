package com.bridgelabz.paymentservice.service.impl;

import com.bridgelabz.paymentservice.dto.PaymentRequest;
import com.bridgelabz.paymentservice.dto.PaymentResponse;
import com.bridgelabz.paymentservice.model.Payment;
import com.bridgelabz.paymentservice.model.PaymentMode;
import com.bridgelabz.paymentservice.model.PaymentStatus;
import com.bridgelabz.paymentservice.repository.PaymentRepository;
import com.bridgelabz.paymentservice.publisher.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private com.razorpay.RazorpayClient razorpayClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentRequest paymentRequest;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRequest = PaymentRequest.builder()
                .appointmentId("APP-0092")
                .patientId("PAT-8802")
                .providerId("PROV-101")
                .amount(750.0)
                .mode(PaymentMode.UPI)
                .currency("INR")
                .notes("Consultation fee payment")
                .build();

        payment = Payment.builder()
                .paymentId("PAY-991")
                .appointmentId("APP-0092")
                .patientId("PAT-8802")
                .amount(750.0)
                .status(PaymentStatus.PAID)
                .transactionId("TXN-RAZOR-445566")
                .build();
    }

    @Test
    void processPayment_Success() {
        when(paymentRepository.save(any())).thenReturn(payment);
        PaymentResponse response = paymentService.processPayment(paymentRequest);
        assertNotNull(response);
        verify(notificationPublisher, atLeastOnce()).publishNotification(any());
    }

    @Test
    void getPaymentByAppointment_Success() {
        when(paymentRepository.findByAppointmentId("APP-0092")).thenReturn(Optional.of(payment));
        PaymentResponse response = paymentService.getPaymentByAppointment("APP-0092");
        assertEquals("PAY-991", response.getPaymentId());
    }

    @Test
    void refundPayment_Success() {
        when(paymentRepository.findById("PAY-991")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenReturn(payment);
        PaymentResponse response = paymentService.refundPayment("PAY-991", "Patient requested cancellation");
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.REFUNDED));
    }

    @Test
    void updatePaymentStatusByAppointment_Success() {
        when(paymentRepository.findByAppointmentId("APP-0092")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenReturn(payment);
        PaymentResponse response = paymentService.updatePaymentStatusByAppointment("APP-0092", PaymentStatus.PAID);
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.PAID));
    }

    @Test
    void getTotalRevenue_Success() {
        when(paymentRepository.findAll()).thenReturn(Collections.singletonList(payment));
        Double revenue = paymentService.getTotalRevenue();
        assertEquals(750.0, revenue);
    }
}
