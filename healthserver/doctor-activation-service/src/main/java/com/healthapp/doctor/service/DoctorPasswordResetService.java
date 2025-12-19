package com.healthapp.doctor.service;

import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

/**
 * Service de réinitialisation de mot de passe avec Keycloak
 *
 * ✅ FONCTIONNALITÉS:
 * 1. Déclenche l'action UPDATE_PASSWORD dans Keycloak
 * 2. Keycloak envoie automatiquement un email au doctor
 * 3. Le doctor clique sur le lien et réinitialise son mot de passe
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorPasswordResetService {

    private final DoctorRepository doctorRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.realm:health-app-realm}")
    private String realm;

    @Value("${keycloak.serverUrl:http://localhost:8080}")
    private String keycloakServerUrl;

    /**
     * Déclencher la réinitialisation de mot de passe via Keycloak
     *
     * ✅ Keycloak envoie automatiquement un email avec un lien de réinitialisation
     * ✅ Plus besoin de gérer les tokens manuellement
     * ✅ Plus sécurisé (géré par Keycloak)
     */
    public void sendPasswordResetEmailForDoctor(String email) {
        log.info("========================================");
        log.info("🔐 PASSWORD RESET REQUEST (KEYCLOAK)");
        log.info("========================================");
        log.info("Email: {}", email);

        try {
            // Vérifier si le doctor existe dans MongoDB
            Doctor doctor = doctorRepository.findByContactEmail(email).orElse(null);

            if (doctor == null) {
                log.warn("⚠️ Doctor not found in MongoDB: {}", email);
                // ⚠️ Ne pas révéler que le doctor n'existe pas (sécurité)
                return;
            }

            // Vérifier si le doctor est activé
            if (!doctor.getIsActivated()) {
                log.warn("⚠️ Doctor account not activated: {}", email);
                // Ne pas envoyer d'email si le compte n'est pas activé
                return;
            }

            log.info("✅ Doctor found: {} (Keycloak ID: {})",
                    doctor.getFullName(), doctor.getUserId());

            // ✅ Déclencher l'action UPDATE_PASSWORD via Keycloak
            sendKeycloakPasswordResetEmail(doctor.getUserId(), email);

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

            // ✅ NOUVELLE VERSION KEYCLOAK → VOID
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
    public boolean isDoctorEligibleForPasswordReset(String email) {
        try {
            Doctor doctor = doctorRepository.findByEmail(email).orElse(null);

            if (doctor == null) {
                log.debug("Doctor not found: {}", email);
                return false;
            }

            if (!doctor.getIsActivated()) {
                log.debug("Doctor not activated: {}", email);
                return false;
            }

            // Vérifier dans Keycloak
            UserResource userResource = keycloak.realm(realm)
                    .users()
                    .get(doctor.getUserId());

            UserRepresentation user = userResource.toRepresentation();

            return user != null && user.isEnabled();

        } catch (Exception e) {
            log.error("Error checking doctor eligibility: {}", e.getMessage());
            return false;
        }
    }
}