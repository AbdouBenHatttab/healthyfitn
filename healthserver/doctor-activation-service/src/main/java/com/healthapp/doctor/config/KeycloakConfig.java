package com.healthapp.doctor.config;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Keycloak Admin Client
 * Permet de créer/gérer les utilisateurs dans Keycloak depuis le service
 */
@Configuration
@Slf4j
public class KeycloakConfig {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.realm}")
    private String adminRealm;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    @Value("${keycloak.admin.client-id}")
    private String adminClientId;

    /**
     * Créer un bean Keycloak Admin Client
     * Ce bean sera utilisé pour gérer les utilisateurs via l'API Keycloak
     */
    @Bean
    public Keycloak keycloakAdminClient() {
        log.info("========================================");
        log.info("🔐 Initializing Keycloak Admin Client");
        log.info("========================================");
        log.info("Server URL: {}", serverUrl);
        log.info("Admin Realm: {}", adminRealm);
        log.info("Target Realm: {}", realm);
        log.info("Admin Username: {}", adminUsername);
        log.info("========================================");

        try {
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(adminRealm)
                    .grantType(OAuth2Constants.PASSWORD)
                    .clientId(adminClientId)
                    .username(adminUsername)
                    .password(adminPassword)
                    .build();

            // Test de connexion
            keycloak.serverInfo().getInfo();
            log.info("✅ Keycloak Admin Client connected successfully");

            return keycloak;

        } catch (Exception e) {
            log.error("❌ Failed to initialize Keycloak Admin Client", e);
            throw new RuntimeException("Could not connect to Keycloak", e);
        }
    }
}