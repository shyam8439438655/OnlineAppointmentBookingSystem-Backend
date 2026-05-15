package com.bridgelabz.notificationservice.service.impl;

import com.bridgelabz.notificationservice.dto.NotificationRequest;
import com.bridgelabz.notificationservice.dto.NotificationResponse;
import com.bridgelabz.notificationservice.model.Notification;
import com.bridgelabz.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private NotificationRequest notificationRequest;
    private Notification notification;

    @BeforeEach
    void setUp() {
        notificationRequest = NotificationRequest.builder()
                .recipientId("PAT-8802")
                .type("BOOKING")
                .title("Appointment Confirmed")
                .message("Your appointment with Dr. Amit Verma is confirmed for tomorrow.")
                .channel("EMAIL")
                .build();

        notification = Notification.builder()
                .notificationId("NOTIF-XY-001")
                .recipientId("PAT-8802")
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();
    }

    @Test
    void send_EmailSuccess() {
        Map<String, String> contactInfo = new HashMap<>();
        contactInfo.put("email", "rajat.b@gmail.com");
        contactInfo.put("phone", "9876543210");

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(contactInfo);
        when(notificationRepository.save(any())).thenReturn(notification);

        NotificationResponse response = notificationService.send(notificationRequest);

        assertNotNull(response);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        verify(notificationRepository, times(1)).save(any());
    }

    @Test
    void getUnreadCount_ReturnsSum() {
        when(notificationRepository.countByRecipientIdAndIsRead("PAT-8802", false)).thenReturn(3L);
        when(notificationRepository.countByRecipientIdAndIsRead("ALL", false)).thenReturn(1L);

        long count = notificationService.getUnreadCount("PAT-8802");

        assertEquals(4L, count);
    }

    @Test
    void markAllRead_UpdatesBothLists() {
        when(notificationRepository.findByRecipientIdAndIsRead("PAT-8802", false)).thenReturn(Collections.singletonList(notification));
        when(notificationRepository.findByRecipientIdAndIsRead("ALL", false)).thenReturn(Collections.emptyList());

        notificationService.markAllRead("PAT-8802");

        verify(notificationRepository, atLeastOnce()).saveAll(any());
    }

    @Test
    void sendSMS_MockLogging() {
        // Since SMS is just a log, we verify the service call completes
        assertDoesNotThrow(() -> notificationService.sendSMS("9876543210", "Your MediBook OTP is 4455"));
    }

    @Test
    void getByRecipient_ReturnsCombinedSortedList() {
        Notification n1 = Notification.builder().sentAt(LocalDateTime.now().minusHours(1)).recipientId("PAT-8802").build();
        Notification n2 = Notification.builder().sentAt(LocalDateTime.now()).recipientId("ALL").build();

        when(notificationRepository.findByRecipientId("PAT-8802")).thenReturn(Collections.singletonList(n1));
        when(notificationRepository.findByRecipientId("ALL")).thenReturn(Collections.singletonList(n2));

        List<NotificationResponse> results = notificationService.getByRecipient("PAT-8802");

        assertEquals(2, results.size());
        // Check sorting (descending by time)
        assertEquals("ALL", results.get(0).getRecipientId());
    }
}
