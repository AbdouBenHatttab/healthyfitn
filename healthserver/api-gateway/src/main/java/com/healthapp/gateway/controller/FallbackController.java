package com.healthapp.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Contrôleur de fallback pour gérer les pannes des microservices
 * Retourne des réponses par défaut quand un service est indisponible
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    /**
     * Fallback pour Auth Service
     * Accepte toutes les méthodes HTTP
     */
    @RequestMapping(value = "/auth", method = {
            org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.PUT,
            org.springframework.web.bind.annotation.RequestMethod.DELETE,
            org.springframework.web.bind.annotation.RequestMethod.PATCH
    })
    public ResponseEntity<Map<String, Object>> authServiceFallback() {
        log.warn("🔴 Auth Service is unavailable - Circuit Breaker activated");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", false,
                        "error", "Service Unavailable",
                        "message", "Le service d'authentification est temporairement indisponible. Veuillez réessayer dans quelques instants.",
                        "service", "auth-service",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    /**
     * Fallback pour User Service
     * Accepte toutes les méthodes HTTP (GET, POST, PUT, DELETE, PATCH)
     */
    @RequestMapping(value = "/user", method = {
            org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.PUT,
            org.springframework.web.bind.annotation.RequestMethod.DELETE,
            org.springframework.web.bind.annotation.RequestMethod.PATCH
    })
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        log.warn("🔴 User Service is unavailable - Circuit Breaker activated");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", false,
                        "error", "Service Unavailable",
                        "message", "Le service utilisateur est temporairement indisponible. Veuillez réessayer dans quelques instants.",
                        "service", "user-service",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    /**
     * Fallback pour Doctor Service
     * Accepte toutes les méthodes HTTP
     */
    @RequestMapping(value = "/doctor", method = {
            org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.PUT,
            org.springframework.web.bind.annotation.RequestMethod.DELETE,
            org.springframework.web.bind.annotation.RequestMethod.PATCH
    })
    public ResponseEntity<Map<String, Object>> doctorServiceFallback() {
        log.warn("🔴 Doctor Service is unavailable - Circuit Breaker activated");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", false,
                        "error", "Service Unavailable",
                        "message", "Le service médecin est temporairement indisponible. Veuillez réessayer dans quelques instants.",
                        "service", "doctor-activation-service",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    /**
     * Fallback générique pour les autres services
     * Accepte toutes les méthodes HTTP
     */
    @RequestMapping(value = "/default", method = {
            org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.PUT,
            org.springframework.web.bind.annotation.RequestMethod.DELETE,
            org.springframework.web.bind.annotation.RequestMethod.PATCH
    })
    public ResponseEntity<Map<String, Object>> defaultFallback() {
        log.warn("🔴 Service unavailable - Circuit Breaker activated");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", false,
                        "error", "Service Unavailable",
                        "message", "Le service est temporairement indisponible. Veuillez réessayer dans quelques instants.",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}