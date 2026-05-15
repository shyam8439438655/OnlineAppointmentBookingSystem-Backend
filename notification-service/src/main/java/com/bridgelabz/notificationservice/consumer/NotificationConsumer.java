package com.bridgelabz.notificationservice.consumer;

import com.bridgelabz.notificationservice.dto.NotificationRequest;
import com.bridgelabz.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${notification.rabbitmq.queue}")
    public void consumeNotification(NotificationRequest request) {
        log.info(">>>> [NOTIFICATION RECEIVED] Title: {}, Type: {}, To: {}", request.getTitle(), request.getType(), request.getRecipientId());
        try {
            notificationService.send(request);
        } catch (Exception e) {
            log.error("CRITICAL: Error processing consumed notification: {}", e.getMessage());
        }
    }
}
