package com.bridgelabz.notificationservice.service.impl;

import com.bridgelabz.notificationservice.dto.*;
import com.bridgelabz.notificationservice.exception.NotificationNotFoundException;
import com.bridgelabz.notificationservice.model.Notification;
import com.bridgelabz.notificationservice.repository.NotificationRepository;
import com.bridgelabz.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;

    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";

    @org.springframework.beans.factory.annotation.Value("${services.auth.url:http://localhost:8081/auth/user/}")
    private String authServiceUrl;

    @Override
    public NotificationResponse send(NotificationRequest request) {
        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .channel(request.getChannel())
                .relatedId(request.getRelatedId())
                .relatedType(request.getRelatedType())
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // Resolve recipient contact info
        log.info("Attempting to resolve contact info for recipientId: {}", request.getRecipientId());
        Map<String, String> contactInfo = resolveContactInfo(request.getRecipientId());
        String email = contactInfo.get(KEY_EMAIL);
        String phone = contactInfo.get(KEY_PHONE);
        log.info("Resolved contact info - Email: {}, Phone: {}", email, phone);

        if ("EMAIL".equalsIgnoreCase(request.getChannel()) || "BOTH".equalsIgnoreCase(request.getChannel())) {
            if (email != null && email.contains("@")) {
                log.info("Sending email to {} with title: {}", email, request.getTitle());
                sendEmail(email, request.getTitle(), request.getMessage());
            } else {
                log.warn("Invalid email for recipient {}: {}. Skipping email send.", request.getRecipientId(), email);
            }
        }
        
        if ("SMS".equalsIgnoreCase(request.getChannel()) || "BOTH".equalsIgnoreCase(request.getChannel())) {
            if (phone != null && !phone.isBlank()) {
                log.info("Sending SMS to {} with message: {}", phone, request.getMessage());
                sendSMS(phone, request.getMessage());
            } else {
                log.warn("No phone number found for recipient {}. Skipping SMS send.", request.getRecipientId());
            }
        }

        return mapToResponse(savedNotification);
    }

    private Map<String, String> resolveContactInfo(String userId) {
        try {
            if ("ALL".equalsIgnoreCase(userId) || "admin".equalsIgnoreCase(userId)) {
                return Map.of(KEY_EMAIL, "admin@medibook.com", KEY_PHONE, "9999999999");
            }
            
            log.info("Resolving contact info for userId: {}", userId);
            Map<String, Object> response = restTemplate.getForObject(authServiceUrl + userId, Map.class);
            if (response != null) {
                return Map.of(
                    KEY_EMAIL, String.valueOf(response.get(KEY_EMAIL)),
                    KEY_PHONE, String.valueOf(response.get(KEY_PHONE))
                );
            }
        } catch (Exception e) {
            log.error("CRITICAL: Failed to resolve contact info for user {}. Reason: {}", userId, e.getMessage());
            if (e instanceof org.springframework.web.client.HttpClientErrorException hce) {
                log.error("HTTP Status: {}, Response Body: {}", 
                    hce.getStatusCode(),
                    hce.getResponseBodyAsString());
            }
        }
        // Fallback or return ID if it looks like an email already (unlikely but safe)
        return Map.of(KEY_EMAIL, userId, KEY_PHONE, "");
    }

    @Override
    public List<NotificationResponse> sendBulk(BulkNotificationRequest request) {
        log.info("Sending bulk notification to {} recipients", request.getRecipientIds().size());
        return request.getRecipientIds().stream()
                .map(id -> mapToNotificationRequest(id, request))
                .map(this::send)
                .toList();
    }

    private NotificationRequest mapToNotificationRequest(String id, BulkNotificationRequest request) {
        return NotificationRequest.builder()
                .recipientId(id.trim())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .channel(request.getChannel())
                .relatedId(request.getRelatedId())
                .relatedType(request.getRelatedType())
                .build();
    }

    @Override
    public void markAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + notificationId));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllRead(String recipientId) {
        List<Notification> unreadPersonal = notificationRepository.findByRecipientIdAndIsRead(recipientId.trim(), false);
        unreadPersonal.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unreadPersonal);
        
        List<Notification> unreadGlobal = notificationRepository.findByRecipientIdAndIsRead("ALL", false);
        unreadGlobal.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unreadGlobal);
    }

    @Override
    public List<NotificationResponse> getByRecipient(String recipientId) {
        String cleanId = recipientId != null ? recipientId.trim() : "";
        List<Notification> personal = notificationRepository.findByRecipientId(cleanId);
        List<Notification> global = notificationRepository.findByRecipientId("ALL");
        
        List<Notification> combined = new java.util.ArrayList<>(personal);
        combined.addAll(global);
        
        return combined.stream()
                .sorted((a, b) -> b.getSentAt().compareTo(a.getSentAt()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public long getUnreadCount(String recipientId) {
        String cleanId = recipientId != null ? recipientId.trim() : "";
        long personalCount = notificationRepository.countByRecipientIdAndIsRead(cleanId, false);
        long globalCount = notificationRepository.countByRecipientIdAndIsRead("ALL", false);
        return personalCount + globalCount;
    }

    @Override
    public void deleteNotification(String notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Override
    public void deleteAll() {
        notificationRepository.deleteAll();
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void sendSMS(String to, String body) {
        log.info("SMS [PROCESSED]: Sent to {} | Message: {}", to, body);
        // Integration with Twilio/Fast2SMS would happen here
    }

    @Override
    public List<NotificationResponse> getAll() {
        return notificationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .recipientId(notification.getRecipientId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .channel(notification.getChannel())
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
                .isRead(notification.getIsRead())
                .sentAt(notification.getSentAt())
                .build();
    }
}