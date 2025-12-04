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

import java.util.List;

/**
 * Service de réinitialisation de mot de passe avec Keycloak
 *
 * ✅ CHANGEMENT MAJEUR:
 * La réinitialisation de mot de passe est maintenant déléguée à Keycloak.
 * Ce service déclenche l'action "UPDATE_PASSWORD" qui envoie un email au doctor.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorPasswordResetService {

    private final DoctorRepository doctorRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

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
            Doctor doctor = doctorRepository.findByEmail(email).orElse(null);

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
            log.info("✅ PASSWORD RESET EMAIL SENT BY KEYCLOAK");
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

            // Récupérer l'utilisateur dans Keycloak
            UserResource userResource = keycloak.realm(realm)
                    .users()
                    .get(keycloakUserId);

            UserRepresentation user = userResource.toRepresentation();

            // Vérifier que l'utilisateur est activé
            if (!user.isEnabled()) {
                log.warn("⚠️ User is disabled in Keycloak: {}", email);
                return;
            }

            // ✅ Déclencher l'action UPDATE_PASSWORD
            // Keycloak enverra automatiquement un email avec un lien de réinitialisation
            userResource.executeActionsEmail(List.of("UPDATE_PASSWORD"));

            log.info("✅ Keycloak password reset email sent to: {}", email);
            log.info("   The user will receive an email with a password reset link");

        } catch (Exception e) {
            log.error("❌ Failed to send Keycloak password reset email", e);
            log.error("   Keycloak User ID: {}", keycloakUserId);
            log.error("   Error: {}", e.getMessage());

            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }

    /**
     * ⚠️ MÉTHODE OBSOLÈTE - Gardée pour compatibilité
     *
     * Avec Keycloak, on n'utilise plus de tokens manuels.
     * Keycloak génère ses propres tokens sécurisés.
     */
    @Deprecated
    public String generateResetToken(String email) {
        log.warn("⚠️ generateResetToken() is deprecated with Keycloak");
        log.warn("   Password reset is now handled entirely by Keycloak");
        return null;
    }

    /**
     * ⚠️ MÉTHODE OBSOLÈTE - Gardée pour compatibilité
     *
     * Avec Keycloak, la validation des tokens est gérée par Keycloak.
     */
    @Deprecated
    public boolean validateResetToken(String token) {
        log.warn("⚠️ validateResetToken() is deprecated with Keycloak");
        log.warn("   Token validation is now handled entirely by Keycloak");
        return false;
    }
}