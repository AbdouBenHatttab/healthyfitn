package com.healthapp.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Discovery Service - Eureka Server
 * 
 * Ce service permet la découverte automatique des microservices.
 * Tous les autres services s'enregistrent ici au démarrage.
 * 
 * URL de la console Eureka: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer  // Active le serveur Eureka
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
        System.out.println("""
            
            ========================================
            🚀 Discovery Service démarré!
            📍 Console Eureka: http://localhost:8761
            ========================================
            """);
    }
}