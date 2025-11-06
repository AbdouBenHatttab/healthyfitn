package com.healthapp.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * NotificationLog - Historique de toutes les notifications envoyées
 * 
 * Cette entité garde une trace de toutes les tentatives d'envoi de notifications
 * pour le debugging et l'audit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_logs")
public class NotificationLog {
    
    @Id
    private String id;
    
    @Indexed
    private NotificationType type;  // EMAIL, SMS, PUSH
    
    @Indexed
    private String recipient;
    
    private String subject;
    
    private String content;
    
    private String templateType;
    
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;
    
    private String errorMessage;
    
    private Integer retryCount;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    private LocalDateTime sentAt;
    
    // Métadonnées supplémentaires
    private String sentBy;  // Service qui a demandé l'envoi
    private String userId;  // ID de l'utilisateur concerné
}
