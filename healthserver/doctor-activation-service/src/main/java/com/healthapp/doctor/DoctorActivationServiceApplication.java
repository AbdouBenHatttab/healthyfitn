package com.healthapp.doctor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Doctor Activation Service
 * 
 * Service dédié à la gestion des médecins:
 * - Enregistrement des médecins (séparé de l'auth normale)
 * - Workflow d'activation par les admins
 * - Gestion du profil médecin
 * - Vérification des credentials médicales
 * 
 * Ce service communique avec:
 * - Auth Service (pour créer les credentials de base)
 * - Notification Service (pour les emails)
 * - User Service (pour les infos utilisateur)
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients  // Active Feign pour appeler les autres services
@EnableMongoAuditing
public class DoctorActivationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoctorActivationServiceApplication.class, args);
        System.out.println("""
            
            ========================================
            🏥 Doctor Activation Service démarré!
            📍 Port: 8083
            📍 Endpoints disponibles:
               POST /api/doctors/register
               GET  /api/doctors/pending (ADMIN)
               POST /api/doctors/activate (ADMIN)
               GET  /api/doctors/profile (DOCTOR)
            ========================================
            """);
    }
}