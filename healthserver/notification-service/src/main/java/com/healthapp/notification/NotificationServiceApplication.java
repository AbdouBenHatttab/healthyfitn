package com.healthapp.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Notification Service
 * 
 * Service dédié à l'envoi de notifications:
 * - Emails (avec templates HTML)
 * - SMS (futur)
 * - Push notifications (futur)
 * 
 * Ce service est appelé par les autres microservices pour envoyer des notifications.
 */
@SpringBootApplication
@EnableDiscoveryClient  // S'enregistre auprès d'Eureka
@EnableAsync           // Active l'envoi asynchrone d'emails
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
        System.out.println("""
            
            ========================================
            📧 Notification Service démarré!
            📍 Port: 8084
            📍 Endpoints disponibles:
               POST /api/notifications/email
               GET  /api/notifications/history
            ========================================
            """);
    }
}