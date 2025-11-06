package com.healthapp.doctor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * ApplicationConfig - Définit les beans utilisés par le service
 */
@Configuration
public class ApplicationConfig {
    
    /**
     * Crée un bean BCryptPasswordEncoder
     * Ce bean sera automatiquement injecté dans les services qui en ont besoin
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}