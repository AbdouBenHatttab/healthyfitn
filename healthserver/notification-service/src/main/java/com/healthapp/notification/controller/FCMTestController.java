package com.healthapp.notification.controller;

import com.healthapp.notification.service.FCMNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller pour tester les notifications FCM
 */
@RestController
@RequestMapping("/api/notifications/fcm/test")
@RequiredArgsConstructor
@Slf4j
public class FCMTestController {
    
    private final FCMNotificationService fcmService;
    
    /**
     * Tester l'envoi d'une notification FCM
     * POST http://localhost:8084/api/notifications/fcm/test
     * Body: {
     *   "token": "VOTRE_FCM_TOKEN",
     *   "title": "Test Notification",
     *   "body": "This is a test message"
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> sendTestNotification(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String title = request.get("title");
        String body = request.get("body");
        
        log.info("📤 Sending test FCM notification");
        
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "TEST");
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            fcmService.sendNotificationToDevice(token, title, body, data);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Notification sent successfully"
            ));
        } catch (Exception e) {
            log.error("❌ Failed to send test notification", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Envoyer une notification à un topic (ex: tous les admins)
     */
    @PostMapping("/topic")
    public ResponseEntity<Map<String, String>> sendTopicNotification(@RequestBody Map<String, String> request) {
        String topic = request.get("topic");
        String title = request.get("title");
        String body = request.get("body");
        
        try {
            fcmService.sendNotificationToTopic(topic, title, body);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Notification sent to topic: " + topic
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}