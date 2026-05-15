package com.bridgelabz.appointmentservice.service.impl;

import com.bridgelabz.appointmentservice.dto.AppointmentRequest;
import com.bridgelabz.appointmentservice.dto.AppointmentResponse;
import com.bridgelabz.appointmentservice.exception.AppointmentNotFoundException;
import com.bridgelabz.appointmentservice.model.Appointment;
import com.bridgelabz.appointmentservice.repository.AppointmentRepository;
import com.bridgelabz.appointmentservice.publisher.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private AppointmentRequest appointmentRequest;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        appointmentRequest = AppointmentRequest.builder()
                .patientId("PAT-8802")
                .providerId("PROV-101")
                .slotId("SLOT-77")
                .appointmentDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(14, 30))
                .endTime(LocalTime.of(15, 0))
                .serviceType("Skin Allergy Consultation")
                .modeOfConsultation("In-Clinic")
                .build();

        appointment = Appointment.builder()
                .appointmentId("APP-0092")
                .patientId("PAT-8802")
                .providerId("PROV-101")
                .slotId("SLOT-77")
                .status("SCHEDULED")
                .build();
    }

    @Test
    void bookAppointment_Success() {
        when(appointmentRepository.save(any())).thenReturn(appointment);
        
        AppointmentResponse response = appointmentService.bookAppointment(appointmentRequest);
        
        assertNotNull(response);
        assertEquals("APP-0092", response.getAppointmentId());
        verify(restTemplate, times(1)).put(anyString(), eq(null));
        verify(notificationPublisher, times(2)).publishNotification(any());
    }

    @Test
    void cancelAppointment_Success() {
        when(appointmentRepository.findById("APP-0092")).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenReturn(appointment);
        
        AppointmentResponse response = appointmentService.cancelAppointment("APP-0092");
        
        assertEquals("CANCELLED", response.getStatus());
        verify(restTemplate, times(1)).put(contains("/unbook"), eq(null));
    }

    @Test
    void completeAppointment_Success() {
        when(appointmentRepository.findById("APP-0092")).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenReturn(appointment);
        
        AppointmentResponse response = appointmentService.completeAppointment("APP-0092");
        
        assertEquals("COMPLETED", response.getStatus());
        verify(restTemplate, times(1)).put(contains("/status?status=PAID"), eq(null));
    }

    @Test
    void getById_NotFound() {
        when(appointmentRepository.findById("invalid")).thenReturn(Optional.empty());
        assertThrows(AppointmentNotFoundException.class, () -> appointmentService.getById("invalid"));
    }

    @Test
    void getByPatient_ReturnsList() {
        when(appointmentRepository.findByPatientId("PAT-8802")).thenReturn(Collections.singletonList(appointment));
        List<AppointmentResponse> results = appointmentService.getByPatient("PAT-8802");
        assertFalse(results.isEmpty());
    }
}
