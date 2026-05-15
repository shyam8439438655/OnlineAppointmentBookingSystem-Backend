package com.bridgelabz.recordservice.service.impl;

import com.bridgelabz.recordservice.dto.MedicalRecordRequest;
import com.bridgelabz.recordservice.dto.MedicalRecordResponse;
import com.bridgelabz.recordservice.dto.MedicalRecordUpdateRequest;
import com.bridgelabz.recordservice.exception.DuplicateMedicalRecordException;
import com.bridgelabz.recordservice.exception.RecordEditingWindowExpiredException;
import com.bridgelabz.recordservice.model.MedicalRecord;
import com.bridgelabz.recordservice.repository.MedicalRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceImplTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @InjectMocks
    private MedicalRecordServiceImpl medicalRecordService;

    private MedicalRecordRequest recordRequest;
    private MedicalRecord record;

    @BeforeEach
    void setUp() {
        recordRequest = MedicalRecordRequest.builder()
                .appointmentId("APP-0092")
                .patientId("PAT-8802")
                .providerId("PROV-101")
                .diagnosis("Seasonal Viral Fever")
                .prescription("Dolo 650mg, Vitamin C")
                .notes("Rest for 3 days and plenty of fluids.")
                .followUpDate(LocalDate.now().plusDays(5))
                .build();

        record = MedicalRecord.builder()
                .recordId("REC-707")
                .appointmentId("APP-0092")
                .createdAt(LocalDateTime.now())
                .diagnosis("Seasonal Viral Fever")
                .build();
    }

    @Test
    void createRecord_Success() {
        when(medicalRecordRepository.findByAppointmentId("APP-0092")).thenReturn(Optional.empty());
        when(medicalRecordRepository.save(any())).thenReturn(record);
        MedicalRecordResponse response = medicalRecordService.createRecord(recordRequest);
        assertNotNull(response);
        assertEquals("REC-707", response.getRecordId());
    }

    @Test
    void createRecord_Duplicate() {
        when(medicalRecordRepository.findByAppointmentId("APP-0092")).thenReturn(Optional.of(record));
        assertThrows(DuplicateMedicalRecordException.class, () -> medicalRecordService.createRecord(recordRequest));
    }

    @Test
    void updateRecord_WithinWindow_Success() {
        when(medicalRecordRepository.findById("REC-707")).thenReturn(Optional.of(record));
        when(medicalRecordRepository.save(any())).thenReturn(record);
        
        MedicalRecordUpdateRequest updateReq = MedicalRecordUpdateRequest.builder()
                .diagnosis("Severe Viral Fever")
                .build();
        
        MedicalRecordResponse response = medicalRecordService.updateRecord("REC-707", updateReq);
        assertNotNull(response);
        verify(medicalRecordRepository).save(any());
    }

    @Test
    void updateRecord_ExpiredWindow_ThrowsException() {
        // Set created time to 25 hours ago
        record.setCreatedAt(LocalDateTime.now().minusHours(25));
        when(medicalRecordRepository.findById("REC-707")).thenReturn(Optional.of(record));
        
        MedicalRecordUpdateRequest updateReq = MedicalRecordUpdateRequest.builder().build();
        assertThrows(RecordEditingWindowExpiredException.class, () -> medicalRecordService.updateRecord("REC-707", updateReq));
    }

    @Test
    void getFollowUpRecords_ReturnsList() {
        LocalDate date = LocalDate.now().plusDays(5);
        when(medicalRecordRepository.findByFollowUpDate(date)).thenReturn(Collections.singletonList(record));
        List<MedicalRecordResponse> results = medicalRecordService.getFollowUpRecords(date);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }
}
