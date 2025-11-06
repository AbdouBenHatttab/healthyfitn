package com.healthapp.notification.dto.response;

import com.healthapp.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistoryResponse {
    
    private String id;
    private NotificationType type;
    private String recipient;
    private String subject;
    private String status;
    private LocalDateTime sentAt;
    private String errorMessage;
}