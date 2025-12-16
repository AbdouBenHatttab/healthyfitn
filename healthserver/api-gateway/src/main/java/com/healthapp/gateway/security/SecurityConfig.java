package com.healthapp.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuration de sécurité pour l'API Gateway
 *
 * ⚠️ IMPORTANT : Le Gateway ne doit PAS valider les JWT
 * La validation est faite par chaque microservice
 *
 * Le Gateway se contente de :
 * 1. Router les requêtes
 * 2. Appliquer le Circuit Breaker
 * 3. Gérer CORS
 * 4. Transmettre l'Authorization header
 */
@Configuration
@EnableWebFluxSecurity
@Slf4j
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("========================================");
        log.info("🔐 Configuring Gateway Security");
        log.info("========================================");
        log.info("✅ All requests are permitted");
        log.info("✅ JWT validation delegated to microservices");
        log.info("========================================");

        return http
                // Désactiver CSRF (API Gateway stateless)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // ✅ CRITIQUE : Autoriser TOUTES les requêtes
                // Chaque microservice fera sa propre validation JWT
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )

                // Désactiver l'authentification par défaut
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                .build();
    }
}