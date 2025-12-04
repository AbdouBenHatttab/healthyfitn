package com.healthapp.user.controller;

import com.healthapp.user.dto.response.ApiResponse;
import com.healthapp.user.entity.User;
import com.healthapp.user.service.KeycloakSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints pour synchroniser les utilisateurs entre Keycloak et MongoDB
 */
@RestController
@RequestMapping("/api/v1/admin/keycloak")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class KeycloakSyncController {

    private final KeycloakSyncService keycloakSyncService;

    /**
     * Synchronise tous les utilisateurs Keycloak vers MongoDB
     */
    @PostMapping("/sync-all")
    public ResponseEntity<ApiResponse<String>> syncAllUsers() {
        log.info("🔄 Admin lance la synchronisation de tous les utilisateurs");

        try {
            keycloakSyncService.syncAllUsersFromKeycloak();
            return ResponseEntity.ok(
                    ApiResponse.success("Tous les utilisateurs ont été synchronisés avec succès", null)
            );
        } catch (Exception e) {
            log.error("❌ Erreur lors de la synchronisation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la synchronisation: " + e.getMessage()));
        }
    }

    /**
     * Synchronise un utilisateur spécifique depuis Keycloak
     */
    @PostMapping("/sync/{keycloakId}")
    public ResponseEntity<ApiResponse<User>> syncUser(@PathVariable String keycloakId) {
        log.info("🔄 Admin lance la synchronisation de l'utilisateur: {}", keycloakId);

        try {
            User user = keycloakSyncService.syncUserFromKeycloak(keycloakId);
            return ResponseEntity.ok(
                    ApiResponse.success("Utilisateur synchronisé avec succès", user)
            );
        } catch (Exception e) {
            log.error("❌ Erreur lors de la synchronisation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la synchronisation: " + e.getMessage()));
        }
    }
}