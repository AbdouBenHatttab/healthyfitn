package com.healthapp.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class ConfigServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
        System.out.println("""
            
            ========================================
            ⚙️  Config Service démarré!
            📍 Port: 8880
            ========================================
            """);
    }
}