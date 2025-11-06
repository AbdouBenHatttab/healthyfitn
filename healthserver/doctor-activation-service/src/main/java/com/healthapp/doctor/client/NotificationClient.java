package com.healthapp.doctor.client;

import com.healthapp.doctor.dto.request.EmailNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * ✅ Client Feign pour communiquer avec le Notification Service
 */
@FeignClient(name = "notification-service", fallback = NotificationClientFallback.class)
public interface NotificationClient {
    
    @PostMapping("/api/notifications/email")
    void sendEmail(@RequestBody EmailNotificationRequest request);
}