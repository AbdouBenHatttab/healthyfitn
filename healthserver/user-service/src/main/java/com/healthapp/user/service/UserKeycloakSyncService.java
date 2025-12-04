package com.healthapp.user.service;

import com.healthapp.user.dto.request.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de synchronisation entre MongoDB et Keycloak pour les utilisateurs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserKeycloakSyncService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    /**
     * Met à jour les informations utilisateur dans Keycloak
     *
     * @param keycloakUserId Le Keycloak User ID (UUID) ou l'email
     * @param request Les données à mettre à jour
     */
    public void updateUserInKeycloak(String keycloakUserId, UpdateUserRequest request) {
        try {
            log.info("🔄 Updating user in Keycloak: {}", keycloakUserId);

            // ✅ Si c'est un UUID, utiliser directement
            UserResource userResource;

            if (isUUID(keycloakUserId)) {
                log.debug("✅ Using UUID directly: {}", keycloakUserId);
                userResource = keycloak.realm(realm)
                        .users()
                        .get(keycloakUserId);
            } else {
                // ⚠️ Si c'est un email, rechercher d'abord l'utilisateur
                log.debug("🔍 Searching user by email: {}", keycloakUserId);
                List<UserRepresentation> users = keycloak.realm(realm)
                        .users()
                        .search(keycloakUserId, true); // exact match

                if (users.isEmpty()) {
                    throw new RuntimeException("User not found in Keycloak with email: " + keycloakUserId);
                }

                String realKeycloakId = users.get(0).getId();
                log.debug("✅ Found user with Keycloak ID: {}", realKeycloakId);

                userResource = keycloak.realm(realm)
                        .users()
                        .get(realKeycloakId);
            }

            UserRepresentation user = userResource.toRepresentation();

            if (user == null) {
                throw new RuntimeException("User not found in Keycloak");
            }

            // Mise à jour des champs
            if (request.getFirstName() != null) {
                user.setFirstName(request.getFirstName());
            }

            if (request.getLastName() != null) {
                user.setLastName(request.getLastName());
            }

            if (request.getEmail() != null) {
                user.setEmail(request.getEmail());
            }

            // Appliquer les modifications
            userResource.update(user);

            log.info("✅ Keycloak user updated: {} ({})", user.getEmail(), user.getId());

        } catch (Exception e) {
            log.error("❌ Failed to update user in Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update user in Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Vérifie si une chaîne est un UUID valide
     */
    private boolean isUUID(String value) {
        if (value == null) {
            return false;
        }
        // UUID format: 8-4-4-4-12 (ex: 8801c8f1-17a0-46a3-a6f1-1c907de7cba1)
        return value.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }
}