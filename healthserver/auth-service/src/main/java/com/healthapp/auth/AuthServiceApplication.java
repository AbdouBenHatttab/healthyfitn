package com.healthapp.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoAuditing
public class AuthServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
        System.out.println("""
            
            ========================================
            🔐 Auth Service démarré!
            📍 Port: 8082
            📊 MongoDB: health_auth_db
            🎯 Endpoints: /api/v1/auth/*
            ========================================
            """);
    }
}
