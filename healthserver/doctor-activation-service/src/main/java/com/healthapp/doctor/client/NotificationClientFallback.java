package com.healthapp.doctor.client;


import com.healthapp.doctor.dto.request.EmailNotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback en cas d'échec du Notification Service
 */
@Component
@Slf4j
public class NotificationClientFallback implements NotificationClient {
    
    @Override
    public void sendEmail(EmailNotificationRequest request) {
        log.error("❌ FAILED TO SEND EMAIL - Notification Service is down!");
        log.error("Email was supposed to be sent to: {}", request.getTo());
        log.error("Subject: {}", request.getSubject());
        
        // On pourrait logger dans une table pour retry plus tard
    }
}