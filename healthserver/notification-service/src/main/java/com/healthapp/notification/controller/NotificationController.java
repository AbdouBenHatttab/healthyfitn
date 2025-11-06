package com.healthapp.notification.controller;

import com.healthapp.notification.dto.request.EmailRequest;
import com.healthapp.notification.dto.response.NotificationHistoryResponse;
import com.healthapp.notification.dto.response.NotificationResponse;
import com.healthapp.notification.entity.NotificationLog;
import com.healthapp.notification.entity.NotificationStatus;
import com.healthapp.notification.repository.NotificationLogRepository;
import com.healthapp.notification.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * NotificationController - API pour envoyer des notifications
 * 
 * Ce controller expose des endpoints REST pour que les autres microservices
 * puissent envoyer des notifications.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final EmailService emailService;
    private final NotificationLogRepository notificationLogRepository;
    
    /**
     * Endpoint principal pour envoyer un email
     * 
     * Utilisé par les autres microservices via Feign Client
     */
    @PostMapping("/email")
    public ResponseEntity<NotificationResponse> sendEmail(@Valid @RequestBody EmailRequest request) {
        log.info("📬 Received email request for: {}", request.getTo());
        
        try {
            // L'envoi est asynchrone, donc on retourne immédiatement
            emailService.sendEmail(request);
            
            NotificationResponse response = NotificationResponse.builder()
                    .notificationId(UUID.randomUUID().toString())
                    .status("PENDING")
                    .message("Email queued for sending")
                    .sentAt(LocalDateTime.now())
                    .recipient(request.getTo())
                    .build();
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            log.error("Failed to queue email", e);
            
            NotificationResponse response = NotificationResponse.builder()
                    .status("FAILED")
                    .message("Failed to queue email: " + e.getMessage())
                    .recipient(request.getTo())
                    .build();
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Récupérer l'historique des notifications
     */
    @GetMapping("/history")
    public ResponseEntity<List<NotificationHistoryResponse>> getNotificationHistory(
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String status) {
        
        List<NotificationLog> logs;
        
        if (recipient != null) {
            logs = notificationLogRepository.findByRecipient(recipient);
        } else if (status != null) {
            logs = notificationLogRepository.findByStatus(NotificationStatus.valueOf(status));
        } else {
            logs = notificationLogRepository.findAll();
        }
        
        List<NotificationHistoryResponse> response = logs.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtenir les statistiques des notifications
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        long totalSent = notificationLogRepository.countByStatus(NotificationStatus.SENT);
        long totalFailed = notificationLogRepository.countByStatus(NotificationStatus.FAILED);
        long totalPending = notificationLogRepository.countByStatus(NotificationStatus.PENDING);
        long totalRetry = notificationLogRepository.countByStatus(NotificationStatus.RETRY);
        
        Map<String, Object> stats = Map.of(
            "total", totalSent + totalFailed + totalPending + totalRetry,
            "sent", totalSent,
            "failed", totalFailed,
            "pending", totalPending,
            "retry", totalRetry,
            "successRate", totalSent > 0 ? (totalSent * 100.0 / (totalSent + totalFailed)) : 0
        );
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "notification-service",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
    
    /**
     * Mapper NotificationLog vers NotificationHistoryResponse
     */
    private NotificationHistoryResponse mapToHistoryResponse(NotificationLog log) {
        return NotificationHistoryResponse.builder()
                .id(log.getId())
                .type(log.getType())
                .recipient(log.getRecipient())
                .subject(log.getSubject())
                .status(log.getStatus().name())
                .sentAt(log.getSentAt())
                .errorMessage(log.getErrorMessage())
                .build();
    }
}