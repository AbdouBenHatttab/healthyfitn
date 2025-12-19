package com.healthapp.user.service;

import com.healthapp.user.dto.request.ChangePasswordRequest;
import com.healthapp.user.entity.User;
import com.healthapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service de changement de mot de passe avec Keycloak
 *
 * ✅ CHANGEMENT MAJEUR:
 * Le changement de mot de passe est maintenant géré par Keycloak.
 * Ce service met à jour le mot de passe dans Keycloak uniquement.
 *
 * ⚠️ LIMITATION IMPORTANTE:
 * L'API Admin Keycloak ne permet PAS de vérifier l'ancien mot de passe.
 * Pour une vraie vérification, utilisez l'une de ces solutions:
 *
 * 1. RECOMMANDÉ: Rediriger vers Keycloak Account Console
 * 2. Alternative: Utiliser l'API OAuth2 Direct Grant (nécessite le mot de passe actuel)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.realm:health-app-realm}")
    private String realm;

    @Value("${keycloak.serverUrl:http://localhost:8080}")
    private String keycloakServerUrl;

    /**
     * Changer le mot de passe dans Keycloak (SANS vérification de l'ancien)
     *
     * ⚠️ LIMITATION:
     * Cette méthode ne peut pas vérifier l'ancien mot de passe via l'API Admin.
     * Elle change directement le mot de passe dans Keycloak.
     *
     * UTILISATION:
     * - Changement par admin
     * - Réinitialisation après validation email
     * - Changement forcé
     */
    public void changePassword(String userId, ChangePasswordRequest request) {
        log.info("========================================");
        log.info("🔐 PASSWORD CHANGE REQUEST (KEYCLOAK)");
        log.info("========================================");
        log.info("User ID: {}", userId);

        // Validation du nouveau mot de passe
        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            log.error("❌ New password is null or empty");
            throw new IllegalArgumentException("New password is required");
        }

        // Validation de la force du mot de passe
        validatePasswordStrength(request.getNewPassword());

        try {
            // Trouver l'utilisateur dans MongoDB
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("❌ User not found with id: {}", userId);
                        return new RuntimeException("User not found with id: " + userId);
                    });

            log.info("✅ User found: email={}, keycloakId={}",
                    user.getEmail(), user.getKeycloakId());

            // ⚠️ AVERTISSEMENT sur la vérification de l'ancien mot de passe
            if (request.getCurrentPassword() != null && !request.getCurrentPassword().isEmpty()) {
                log.warn("⚠️ ========================================");
                log.warn("⚠️ SECURITY WARNING");
                log.warn("⚠️ ========================================");
                log.warn("⚠️ Current password verification is NOT supported with Keycloak Admin API");
                log.warn("⚠️ The old password cannot be verified - password will be changed directly");
                log.warn("⚠️ RECOMMENDATION: Use Keycloak Account Console or OAuth2 Direct Grant");
                log.warn("⚠️ ========================================");
            }

            // ✅ Mettre à jour le mot de passe dans Keycloak
            updateKeycloakPassword(user.getKeycloakId(), request.getNewPassword());

            log.info("========================================");
            log.info("✅ PASSWORD CHANGED SUCCESSFULLY IN KEYCLOAK");
            log.info("========================================");
            log.info("User: {} ({})", user.getFullName(), user.getEmail());
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ Failed to change password", e);
            throw new RuntimeException("Failed to update password: " + e.getMessage(), e);
        }
    }

    /**
     * Mettre à jour le mot de passe dans Keycloak via Admin API
     */
    private void updateKeycloakPassword(String keycloakUserId, String newPassword) {
        try {
            log.info("🔐 Updating password in Keycloak");
            log.info("   Keycloak User ID: {}", keycloakUserId);

            // Récupérer l'utilisateur dans Keycloak
            UserResource userResource = keycloak.realm(realm)
                    .users()
                    .get(keycloakUserId);

            UserRepresentation user = userResource.toRepresentation();

            if (user == null) {
                throw new RuntimeException("User not found in Keycloak");
            }

            if (!user.isEnabled()) {
                throw new RuntimeException("User account is disabled");
            }

            // Créer la représentation du nouveau mot de passe
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false); // Mot de passe permanent (non temporaire)

            // ✅ Mettre à jour le mot de passe
            userResource.resetPassword(credential);

            log.info("✅ Password updated successfully in Keycloak");
            log.info("   User: {}", user.getUsername());
            log.info("   Email: {}", user.getEmail());

        } catch (Exception e) {
            log.error("❌ Failed to update password in Keycloak", e);
            log.error("   Keycloak User ID: {}", keycloakUserId);
            log.error("   Error: {}", e.getMessage());

            throw new RuntimeException("Failed to update Keycloak password: " + e.getMessage(), e);
        }
    }

    /**
     * Valider la force du mot de passe
     *
     * ⚠️ Cette validation est côté backend, mais Keycloak peut aussi
     * avoir ses propres règles de validation de mot de passe.
     */
    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        // Vérifier la présence de différents types de caractères
        boolean hasUpperCase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowerCase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasUpperCase || !hasLowerCase || !hasDigit) {
            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
            );
        }
    }

    /**
     * Obtenir l'URL de changement de mot de passe Keycloak Account Console
     *
     * RECOMMANDÉ: Rediriger l'utilisateur vers cette URL pour un changement
     * de mot de passe sécurisé avec vérification de l'ancien mot de passe.
     */
    public String getKeycloakPasswordChangeUrl() {
        return String.format(
                "%s/realms/%s/account/password",
                keycloakServerUrl,
                realm
        );
    }

    /**
     * Définir un mot de passe temporaire (pour réinitialisation)
     * L'utilisateur devra changer ce mot de passe à sa prochaine connexion
     */
    public void setTemporaryPassword(String userId, String temporaryPassword) {
        try {
            log.info("🔐 Setting temporary password for user: {}", userId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserResource userResource = keycloak.realm(realm)
                    .users()
                    .get(user.getKeycloakId());

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(temporaryPassword);
            credential.setTemporary(true); // ✅ Temporaire - l'utilisateur devra le changer

            userResource.resetPassword(credential);

            log.info("✅ Temporary password set successfully");

        } catch (Exception e) {
            log.error("❌ Failed to set temporary password", e);
            throw new RuntimeException("Failed to set temporary password: " + e.getMessage(), e);
        }
    }

    /**
     * Forcer l'utilisateur à changer son mot de passe à la prochaine connexion
     */
    public void requirePasswordChange(String userId) {
        try {
            log.info("🔐 Requiring password change for user: {}", userId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserResource userResource = keycloak.realm(realm)
                    .users()
                    .get(user.getKeycloakId());

            UserRepresentation keycloakUser = userResource.toRepresentation();
            keycloakUser.getRequiredActions().add("UPDATE_PASSWORD");

            userResource.update(keycloakUser);

            log.info("✅ Password change required successfully");

        } catch (Exception e) {
            log.error("❌ Failed to require password change", e);
            throw new RuntimeException("Failed to require password change: " + e.getMessage(), e);
        }
    }
}