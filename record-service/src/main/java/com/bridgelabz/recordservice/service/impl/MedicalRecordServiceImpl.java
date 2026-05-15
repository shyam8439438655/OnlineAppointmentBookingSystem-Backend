package com.bridgelabz.recordservice.service.impl;

import com.bridgelabz.recordservice.dto.MedicalRecordRequest;
import com.bridgelabz.recordservice.dto.MedicalRecordResponse;
import com.bridgelabz.recordservice.dto.MedicalRecordUpdateRequest;
import com.bridgelabz.recordservice.exception.DuplicateMedicalRecordException;
import com.bridgelabz.recordservice.exception.MedicalRecordNotFoundException;
import com.bridgelabz.recordservice.exception.RecordEditingWindowExpiredException;
import com.bridgelabz.recordservice.model.MedicalRecord;
import com.bridgelabz.recordservice.repository.MedicalRecordRepository;
import com.bridgelabz.recordservice.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    private static final String RECORD_NOT_FOUND_ID = "Medical record not found with id: ";

    @Override
    public MedicalRecordResponse createRecord(MedicalRecordRequest request) {
        if (medicalRecordRepository.findByAppointmentId(request.getAppointmentId()).isPresent()) {
            throw new DuplicateMedicalRecordException(
                    "Medical record already exists for appointmentId: " + request.getAppointmentId()
            );
        }

        MedicalRecord medicalRecord = MedicalRecord.builder()
                .appointmentId(request.getAppointmentId())
                .patientId(request.getPatientId())
                .providerId(request.getProviderId())
                .diagnosis(request.getDiagnosis())
                .prescription(request.getPrescription())
                .notes(request.getNotes())
                .attachmentUrl(request.getAttachmentUrl())
                .followUpDate(request.getFollowUpDate())
                .build();

        return mapToResponse(medicalRecordRepository.save(medicalRecord));
    }

    @Override
    public MedicalRecordResponse getRecordByAppointment(String appointmentId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(
                        "Medical record not found for appointmentId: " + appointmentId));

        return mapToResponse(medicalRecord);
    }

    @Override
    public List<MedicalRecordResponse> getRecordsByPatient(String patientId) {
        return medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<MedicalRecordResponse> getRecordsByProvider(String providerId) {
        return medicalRecordRepository.findByProviderId(providerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public MedicalRecordResponse updateRecord(String recordId, MedicalRecordUpdateRequest request) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(RECORD_NOT_FOUND_ID + recordId));

        if (medicalRecord.getCreatedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            throw new RecordEditingWindowExpiredException("The editing window (24 hours) for this medical record has expired.");
        }

        medicalRecord.setDiagnosis(request.getDiagnosis());
        medicalRecord.setPrescription(request.getPrescription());
        medicalRecord.setNotes(request.getNotes());
        medicalRecord.setAttachmentUrl(request.getAttachmentUrl());
        medicalRecord.setFollowUpDate(request.getFollowUpDate());
        medicalRecord.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(medicalRecordRepository.save(medicalRecord));
    }

    @Override
    public void deleteRecord(String recordId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(RECORD_NOT_FOUND_ID + recordId));

        medicalRecordRepository.delete(medicalRecord);
    }

    @Override
    public MedicalRecordResponse getRecordById(String recordId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(RECORD_NOT_FOUND_ID + recordId));

        return mapToResponse(medicalRecord);
    }

    @Override
    public List<MedicalRecordResponse> getFollowUpRecords(LocalDate followUpDate) {
        return medicalRecordRepository.findByFollowUpDate(followUpDate)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public long getRecordCount(String patientId) {
        return medicalRecordRepository.countByPatientId(patientId);
    }

    @Override
    public MedicalRecordResponse attachDocument(String recordId, String attachmentUrl) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(RECORD_NOT_FOUND_ID + recordId));

        if (medicalRecord.getCreatedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            throw new RecordEditingWindowExpiredException("The editing window (24 hours) for this medical record has expired.");
        }

        medicalRecord.setAttachmentUrl(attachmentUrl);
        medicalRecord.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(medicalRecordRepository.save(medicalRecord));
    }

    @Override
    public List<MedicalRecordResponse> getAllRecords() {
        return medicalRecordRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deleteAll() {
        medicalRecordRepository.deleteAll();
    }

    private MedicalRecordResponse mapToResponse(MedicalRecord medicalRecord) {
        return MedicalRecordResponse.builder()
                .recordId(medicalRecord.getRecordId())
                .appointmentId(medicalRecord.getAppointmentId())
                .patientId(medicalRecord.getPatientId())
                .providerId(medicalRecord.getProviderId())
                .diagnosis(medicalRecord.getDiagnosis())
                .prescription(medicalRecord.getPrescription())
                .notes(medicalRecord.getNotes())
                .attachmentUrl(medicalRecord.getAttachmentUrl())
                .followUpDate(medicalRecord.getFollowUpDate())
                .createdAt(medicalRecord.getCreatedAt())
                .updatedAt(medicalRecord.getUpdatedAt())
                .build();
    }
}