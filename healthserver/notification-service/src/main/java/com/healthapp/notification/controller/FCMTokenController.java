package com.healthapp.notification.controller;

import com.healthapp.notification.dto.request.FCMTokenRequest;
import com.healthapp.notification.entity.UserFcmToken;
import com.healthapp.notification.repository.UserFcmTokenRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * FCMTokenController - Gérer les tokens FCM des utilisateurs
 */
@RestController
@RequestMapping("/api/notifications/fcm")
@RequiredArgsConstructor
@Slf4j
public class FCMTokenController {
    
    private final UserFcmTokenRepository fcmTokenRepository;
    
    /**
     * Sauvegarder ou mettre à jour le token FCM
     * ✅ Appelé par l'app mobile après login
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> saveFcmToken(
            @Valid @RequestBody FCMTokenRequest request,
            Principal authentication) {
        
        String userId = authentication.getName();  // Extract from JWT
        
        log.info("💾 Saving FCM token for user: {}", userId);
        
        // Chercher si un token existe déjà
        UserFcmToken existingToken = fcmTokenRepository.findByUserId(userId)
                .orElse(null);
        
        if (existingToken != null) {
            // Mettre à jour le token existant
            existingToken.setFcmToken(request.getFcmToken());
            existingToken.setDeviceType(request.getDeviceType());
            existingToken.setDeviceModel(request.getDeviceModel());
            existingToken.setLastUpdated(LocalDateTime.now());
            existingToken.setIsActive(true);
            fcmTokenRepository.save(existingToken);
            
            log.info("✅ FCM token updated for user: {}", userId);
        } else {
            // Créer un nouveau token
            UserFcmToken newToken = UserFcmToken.builder()
                    .userId(userId)
                    .fcmToken(request.getFcmToken())
                    .deviceType(request.getDeviceType())
                    .deviceModel(request.getDeviceModel())
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .lastUpdated(LocalDateTime.now())
                    .build();
            
            fcmTokenRepository.save(newToken);
            log.info("✅ FCM token created for user: {}", userId);
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "FCM token saved successfully"
        ));
    }
    
    /**
     * Supprimer le token FCM (lors du logout)
     */
    @DeleteMapping("/token")
    public ResponseEntity<Map<String, String>> deleteFcmToken(Principal authentication) {
        String userId = authentication.getName();
        
        log.info("🗑️ Deleting FCM token for user: {}", userId);
        
        fcmTokenRepository.deleteByUserId(userId);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "FCM token deleted successfully"
        ));
    }
    
    /**
     * Récupérer le token FCM d'un utilisateur
     */
    @GetMapping("/token/{userId}")
    public ResponseEntity<UserFcmToken> getFcmToken(@PathVariable String userId) {
        UserFcmToken token = fcmTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("FCM token not found for user: " + userId));
        
        return ResponseEntity.ok(token);
    }
}