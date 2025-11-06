package com.healthapp.notification.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Firebase Cloud Messaging Service
 * ✅ 100% GRATUIT (pas de limite pour les notifications push)
 * ✅ Supporte Android, iOS et Web
 */
@Service
@Slf4j
public class FCMNotificationService {
    
    @Value("${fcm.credentials.path:firebase-service-account.json}")
    private String credentialsPath;
    
    /**
     * Initialiser Firebase au démarrage de l'application
     */
    @PostConstruct
    public void initialize() {
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(
                            new ClassPathResource(credentialsPath).getInputStream()))
                    .build();
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase initialized successfully");
            }
        } catch (IOException e) {
            log.error("❌ Failed to initialize Firebase: {}", e.getMessage());
        }
    }
    
    /**
     * Envoyer une notification à un appareil spécifique
     * @param token FCM token de l'appareil
     * @param title Titre de la notification
     * @param body Corps du message
     * @param data Données additionnelles (optionnel)
     */
    public void sendNotificationToDevice(String token, String title, String body, Map<String, String> data) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data != null ? data : new HashMap<>())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setColor("#4CAF50")  // Couleur verte
                                    .build())
                            .build())
                    .build();
            
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ Notification sent successfully to device: {}", response);
            
        } catch (FirebaseMessagingException e) {
            log.error("❌ Failed to send notification: {}", e.getMessage());
            throw new RuntimeException("Failed to send notification", e);
        }
    }
    
    /**
     * Envoyer une notification à un topic (plusieurs utilisateurs)
     * Exemple: Notifier tous les admins
     */
    public void sendNotificationToTopic(String topic, String title, String body) {
        try {
            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ Notification sent to topic '{}': {}", topic, response);
            
        } catch (FirebaseMessagingException e) {
            log.error("❌ Failed to send notification to topic: {}", e.getMessage());
        }
    }
    
    /**
     * Notification pour nouveau doctor en attente
     */
    public void notifyAdminNewDoctor(String adminFcmToken, String doctorName, String doctorEmail) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "DOCTOR_REGISTRATION");
        data.put("doctorName", doctorName);
        data.put("doctorEmail", doctorEmail);
        data.put("action", "REVIEW_DOCTOR");
        
        sendNotificationToDevice(
                adminFcmToken,
                "🏥 Nouveau médecin en attente",
                "Dr. " + doctorName + " attend votre approbation",
                data
        );
    }
    
    /**
     * Notification pour doctor approuvé
     */
    public void notifyDoctorApproved(String doctorFcmToken, String doctorName) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "DOCTOR_APPROVED");
        data.put("action", "OPEN_APP");
        
        sendNotificationToDevice(
                doctorFcmToken,
                "✅ Compte activé !",
                "Félicitations Dr. " + doctorName + ", votre compte a été approuvé !",
                data
        );
    }
    
    /**
     * S'abonner à un topic
     */
    public void subscribeToTopic(String fcmToken, String topic) {
        try {
            TopicManagementResponse response = FirebaseMessaging.getInstance()
                    .subscribeToTopic(java.util.List.of(fcmToken), topic);
            
            log.info("✅ Subscribed to topic '{}': {} success, {} errors", 
                     topic, response.getSuccessCount(), response.getFailureCount());
                     
        } catch (FirebaseMessagingException e) {
            log.error("❌ Failed to subscribe to topic: {}", e.getMessage());
        }
    }
}
