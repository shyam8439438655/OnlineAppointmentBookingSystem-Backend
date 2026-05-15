package com.bridgelabz.recordservice.scheduler;

import com.bridgelabz.recordservice.dto.NotificationRequest;
import com.bridgelabz.recordservice.model.MedicalRecord;
import com.bridgelabz.recordservice.publisher.NotificationPublisher;
import com.bridgelabz.recordservice.repository.MedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecordScheduler {

    private final MedicalRecordRepository recordRepository;
    private final NotificationPublisher notificationPublisher;

    // Run daily at 9:00 AM to send follow-up reminders
    @Scheduled(cron = "0 0 9 * * *")
    public void sendFollowUpReminders() {
        LocalDate today = LocalDate.now();
        log.info("Starting Follow-up reminder job for date: {}", today);
        
        List<MedicalRecord> followUpRecords = recordRepository.findByFollowUpDate(today);
        int count = 0;

        for (MedicalRecord medicalRecord : followUpRecords) {
            log.info("Sending follow-up reminder to patient {} for appointment {}", 
                medicalRecord.getPatientId(), medicalRecord.getAppointmentId());
            
            notificationPublisher.publishNotification(NotificationRequest.builder()
                    .recipientId(medicalRecord.getPatientId())
                    .type("FOLLOWUP")
                    .title("Follow-up Reminder")
                    .message("This is a reminder for your follow-up visit regarding your diagnosis: " + medicalRecord.getDiagnosis())
                    .channel("BOTH")
                    .relatedId(medicalRecord.getAppointmentId())
                    .relatedType("APPOINTMENT")
                    .build());
            count++;
        }
        log.info("Follow-up reminder job finished. Sent {} notifications.", count);
    }
}
