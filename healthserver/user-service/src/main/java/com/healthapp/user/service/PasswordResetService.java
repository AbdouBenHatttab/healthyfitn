package com.healthapp.user.service;

import com.healthapp.user.entity.User;
import com.healthapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Service de réinitialisation de mot de passe avec Keycloak
 *
 * ✅ FONCTIONNALITÉS:
 * 1. Déclenche l'action UPDATE_PASSWORD dans Keycloak
 * 2. Keycloak envoie automatiquement un email à l'utilisateur
 * 3. L'utilisateur clique sur le lien et réinitialise son mot de passe
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;

    /**
     * Déclencher la réinitialisation de mot de passe via Keycloak
     *
     * ✅ Keycloak envoie automatiquement un email avec un lien de réinitialisation
     * ✅ Plus besoin de gérer les tokens manuellement
     * ✅ Plus sécurisé (géré par Keycloak)
     */
    public void sendPasswordResetEmailForUser(String email) {
        log.info("========================================");
        log.info("🔐 PASSWORD RESET REQUEST (KEYCLOAK)");
        log.info("========================================");
        log.info("Email: {}", email);

        try {
            // Vérifier si l'utilisateur existe dans MongoDB
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                log.warn("⚠️ User not found in MongoDB: {}", email);
                // ⚠️ Ne pas révéler que l'utilisateur n'existe pas (sécurité)
                return;
            }

            // Vérifier si l'utilisateur est activé
            if (!user.getIsActivated()) {
                log.warn("⚠️ User account not activated: {}", email);
                // Ne pas envoyer d'email si le compte n'est pas activé
                return;
            }

            log.info("✅ User found: {} (Keycloak ID: {})",
                    user.getFullName(), user.getKeycloakId());

            // ✅ Déclencher l'action UPDATE_PASSWORD via Keycloak
            sendKeycloakPasswordResetEmail(user.getKeycloakId(), email);

            log.info("========================================");
            log.info("✅ PASSWORD RESET EMAIL TRIGGERED IN KEYCLOAK");
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ Failed to trigger password reset", e);
            // Ne pas propager l'erreur pour ne pas révéler si le compte existe
        }
    }

    /**
     * Envoyer l'email de réinitialisation via Keycloak Admin API
     */
    private void sendKeycloakPasswordResetEmail(String keycloakUserId, String email) {
        try {
            log.info("📧 Triggering Keycloak password reset action");
            log.info("   Keycloak User ID: {}", keycloakUserId);

            UserResource userResource = keycloak.realm(realm)
                    .users()
                    .get(keycloakUserId);

            UserRepresentation user = userResource.toRepresentation();

            if (user == null) {
                log.error("❌ User not found in Keycloak: {}", keycloakUserId);
                throw new RuntimeException("User not found in Keycloak");
            }

            if (!user.isEnabled()) {
                log.warn("⚠️ User is disabled in Keycloak: {}", email);
                throw new RuntimeException("User account is disabled");
            }

            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                log.error("❌ User has no email in Keycloak: {}", keycloakUserId);
                throw new RuntimeException("User has no email configured");
            }

            // ✅ Déclencher l'action UPDATE_PASSWORD
            userResource.executeActionsEmail(
                    Collections.singletonList("UPDATE_PASSWORD")
            );

            log.info("✅ Keycloak password reset email sent successfully to: {}", email);
            log.info("   The user will receive an email with a password reset link");

        } catch (Exception e) {
            log.error("❌ Failed to send Keycloak password reset email", e);
            log.error("   Keycloak User ID: {}", keycloakUserId);
            log.error("   Error: {}", e.getMessage());

            throw new RuntimeException(
                    "Failed to send password reset email: " + e.getMessage(), e
            );
        }
    }

    /**
     * Obtenir l'URL de réinitialisation de mot de passe Keycloak
     * Alternative: Rediriger l'utilisateur vers cette page
     */
    public String getPasswordResetUrl() {
        return String.format(
                "%s/realms/%s/login-actions/reset-credentials",
                keycloakServerUrl,
                realm
        );
    }

    /**
     * Vérifier si un utilisateur existe et est activé
     * Utilisé pour valider avant d'envoyer l'email
     */
    public boolean isUserEligibleForPasswordReset(String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                log.debug("User not found: {}", email);
                return false;
            }

            if (!user.getIsActivated()) {
                log.debug("User not activated: {}", email);
                return false;
            }

            // Vérifier dans Keycloak
            UserResource userResource = keycloak.realm(realm)
                    .users()
                    .get(user.getKeycloakId());

            UserRepresentation keycloakUser = userResource.toRepresentation();

            return keycloakUser != null && keycloakUser.isEnabled();

        } catch (Exception e) {
            log.error("Error checking user eligibility: {}", e.getMessage());
            return false;
        }
    }
}