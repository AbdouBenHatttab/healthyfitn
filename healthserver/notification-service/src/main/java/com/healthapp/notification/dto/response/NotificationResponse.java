package com.healthapp.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    
    private String notificationId;
    private String status;  // SUCCESS, FAILED, PENDING
    private String message;
    private LocalDateTime sentAt;
    private String recipient;
}
