package com.healthapp.notification.repository;

import com.healthapp.notification.entity.NotificationLog;
import com.healthapp.notification.entity.NotificationStatus;
import com.healthapp.notification.entity.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {
    
    // Trouver toutes les notifications par destinataire
    List<NotificationLog> findByRecipient(String recipient);
    
    // Trouver les notifications par statut
    List<NotificationLog> findByStatus(NotificationStatus status);
    
    // Trouver les notifications échouées
    List<NotificationLog> findByStatusAndRetryCountLessThan(NotificationStatus status, Integer maxRetry);
    
    // Trouver les notifications par type
    List<NotificationLog> findByType(NotificationType type);
    
    // Trouver les notifications dans une période
    List<NotificationLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Compter les notifications par statut
    long countByStatus(NotificationStatus status);
    
    // Trouver les notifications d'un utilisateur
    List<NotificationLog> findByUserId(String userId);
}