package com.healthapp.user.service;

import com.healthapp.user.dto.request.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de synchronisation entre MongoDB et Keycloak pour les utilisateurs
 *
 * ✅ CORRECTION: Utilise maintenant le bean Keycloak configuré dans KeycloakConfig
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserKeycloakSyncService {

    private final Keycloak keycloak;  // ✅ Injecté depuis KeycloakConfig

    @Value("${keycloak.realm:health-app-realm}")
    private String realm;

    /**
     * Met à jour les informations utilisateur dans Keycloak
     *
     * @param keycloakUserId Le Keycloak User ID (UUID) ou l'email
     * @param request Les données à mettre à jour
     */
    public void updateUserInKeycloak(String keycloakUserId, UpdateUserRequest request) {
        log.info("🔄 Updating user in Keycloak: {}", keycloakUserId);

        try {
            // ========================================
            // ÉTAPE 1: Récupérer l'utilisateur
            // ========================================
            UserResource userResource = getUserResource(keycloakUserId);
            UserRepresentation user = userResource.toRepresentation();

            if (user == null) {
                throw new RuntimeException("User not found in Keycloak");
            }

            log.debug("✅ Found user in Keycloak: {} ({})", user.getEmail(), user.getId());

            // ========================================
            // ÉTAPE 2: Mettre à jour les champs
            // ========================================
            boolean hasChanges = false;

            if (request.getFirstName() != null && !request.getFirstName().equals(user.getFirstName())) {
                user.setFirstName(request.getFirstName());
                hasChanges = true;
                log.debug("📝 Updated firstName: {}", request.getFirstName());
            }

            if (request.getLastName() != null && !request.getLastName().equals(user.getLastName())) {
                user.setLastName(request.getLastName());
                hasChanges = true;
                log.debug("📝 Updated lastName: {}", request.getLastName());
            }

            if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
                user.setEmail(request.getEmail());
                hasChanges = true;
                log.debug("📝 Updated email: {}", request.getEmail());
            }

            // ========================================
            // ÉTAPE 3: Mettre à jour le phoneNumber dans les attributs
            // ========================================
            if (request.getPhoneNumber() != null) {
                Map<String, List<String>> attributes = user.getAttributes();
                if (attributes == null) {
                    attributes = new HashMap<>();
                }

                String existingPhone = getAttributeValue(attributes, "phoneNumber");
                if (!request.getPhoneNumber().equals(existingPhone)) {
                    attributes.put("phoneNumber", Collections.singletonList(request.getPhoneNumber()));
                    user.setAttributes(attributes);
                    hasChanges = true;
                    log.debug("📱 Updated phoneNumber: {}", request.getPhoneNumber());
                }
            }

            // ========================================
            // ÉTAPE 4: Appliquer les modifications si nécessaire
            // ========================================
            if (hasChanges) {
                userResource.update(user);
                log.info("✅ Keycloak user updated successfully: {} ({})", user.getEmail(), user.getId());
            } else {
                log.info("ℹ️ No changes detected, Keycloak not updated");
            }

        } catch (Exception e) {
            log.error("❌ Failed to update user in Keycloak: {}", e.getMessage());
            log.error("   Keycloak User ID/Email: {}", keycloakUserId);
            log.error("   Error details:", e);

            // ⚠️ NE PAS THROW - Permettre au service de continuer
            // MongoDB est déjà mis à jour, Keycloak sync est optionnel
            log.warn("⚠️ Continuing without Keycloak update (MongoDB already updated)");
        }
    }

    /**
     * Récupère la UserResource depuis Keycloak
     * Gère à la fois les UUID et les emails
     */
    private UserResource getUserResource(String identifier) {
        try {
            if (isUUID(identifier)) {
                // C'est un UUID, utiliser directement
                log.debug("✅ Using UUID directly: {}", identifier);
                return keycloak.realm(realm)
                        .users()
                        .get(identifier);
            } else {
                // C'est un email, rechercher d'abord
                log.debug("🔍 Searching user by email: {}", identifier);
                List<UserRepresentation> users = keycloak.realm(realm)
                        .users()
                        .search(identifier, true); // exact match

                if (users.isEmpty()) {
                    throw new RuntimeException("User not found in Keycloak with email: " + identifier);
                }

                String keycloakId = users.get(0).getId();
                log.debug("✅ Found user with Keycloak ID: {}", keycloakId);

                return keycloak.realm(realm)
                        .users()
                        .get(keycloakId);
            }
        } catch (Exception e) {
            log.error("❌ Failed to get user resource from Keycloak: {}", e.getMessage());
            throw new RuntimeException("Failed to get user from Keycloak: " + e.getMessage(), e);
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

    /**
     * Extrait une valeur d'attribut depuis Keycloak
     */
    private String getAttributeValue(Map<String, List<String>> attributes, String key) {
        if (attributes == null || !attributes.containsKey(key)) {
            return null;
        }
        List<String> values = attributes.get(key);
        return values.isEmpty() ? null : values.get(0);
    }

    /**
     * Vérifie si un utilisateur existe dans Keycloak
     */
    public boolean userExistsInKeycloak(String email) {
        try {
            List<UserRepresentation> users = keycloak.realm(realm)
                    .users()
                    .search(email, true);
            return !users.isEmpty();
        } catch (Exception e) {
            log.error("❌ Error checking user existence in Keycloak: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Récupère un utilisateur depuis Keycloak par email
     */
    public UserRepresentation getUserByEmail(String email) {
        try {
            List<UserRepresentation> users = keycloak.realm(realm)
                    .users()
                    .search(email, true);

            if (users.isEmpty()) {
                return null;
            }

            return users.get(0);
        } catch (Exception e) {
            log.error("❌ Error retrieving user from Keycloak: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Récupère un utilisateur depuis Keycloak par ID
     */
    public UserRepresentation getUserById(String keycloakId) {
        try {
            return keycloak.realm(realm)
                    .users()
                    .get(keycloakId)
                    .toRepresentation();
        } catch (Exception e) {
            log.error("❌ Error retrieving user from Keycloak: {}", e.getMessage());
            return null;
        }
    }
}