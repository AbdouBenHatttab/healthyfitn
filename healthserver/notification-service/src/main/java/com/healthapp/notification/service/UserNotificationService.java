
package com.healthapp.notification.service;

import com.healthapp.notification.entity.UserFcmToken;
import com.healthapp.notification.repository.UserFcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserNotificationService {
    
    private final UserFcmTokenRepository fcmTokenRepository;
    private final FCMNotificationService fcmService;
    
    /**
     * Envoyer une notification à un utilisateur spécifique
     */
    public void sendNotificationToUser(String userId, String title, String body, Map<String, String> data) {
        log.info("📤 Sending notification to user: {}", userId);
        
        UserFcmToken token = fcmTokenRepository.findByUserId(userId)
                .orElse(null);
        
        if (token != null && token.getIsActive()) {
            try {
                fcmService.sendNotificationToDevice(token.getFcmToken(), title, body, data);
                log.info("✅ Notification sent to user: {}", userId);
            } catch (Exception e) {
                log.error("❌ Failed to send notification to user: {}", userId, e);
                // Marquer le token comme inactif si l'envoi échoue
                token.setIsActive(false);
                fcmTokenRepository.save(token);
            }
        } else {
            log.warn("⚠️ No active FCM token found for user: {}", userId);
        }
    }
    
    /**
     * Notifier un doctor que son compte a été approuvé
     */
    public void notifyDoctorApproved(String doctorId, String doctorName) {
        Map<String, String> data = Map.of(
            "type", "DOCTOR_APPROVED",
            "action", "OPEN_DASHBOARD"
        );
        
        sendNotificationToUser(
            doctorId,
            "✅ Compte activé !",
            "Félicitations Dr. " + doctorName + ", votre compte a été approuvé !",
            data
        );
    }
    
    /**
     * Notifier les admins d'un nouveau doctor en attente
     */
    public void notifyAdminsNewDoctor(String doctorName, String doctorEmail) {
        log.info("📢 Notifying admins about new doctor: {}", doctorEmail);
        
        // Envoyer à un topic "admins"
        fcmService.sendNotificationToTopic(
            "admins",
            "🏥 Nouveau médecin en attente",
            "Dr. " + doctorName + " attend votre approbation"
        );
    }
}
