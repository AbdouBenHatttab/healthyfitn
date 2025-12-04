package com.healthapp.doctor.service;

import com.healthapp.doctor.dto.request.ChangePasswordRequest;
import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;

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
public class DoctorPasswordService {

    private final DoctorRepository doctorRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.server-url}")
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
    public void changePassword(String doctorId, ChangePasswordRequest request) {
        log.info("========================================");
        log.info("🔐 PASSWORD CHANGE REQUEST (KEYCLOAK)");
        log.info("========================================");
        log.info("Doctor ID: {}", doctorId);

        // Validation du nouveau mot de passe
        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            log.error("❌ New password is null or empty");
            throw new IllegalArgumentException("New password is required");
        }

        // Validation de la force du mot de passe
        validatePasswordStrength(request.getNewPassword());

        try {
            // Trouver le doctor dans MongoDB
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> {
                        log.error("❌ Doctor not found with id: {}", doctorId);
                        return new RuntimeException("Doctor not found with id: " + doctorId);
                    });

            log.info("✅ Doctor found: email={}, keycloakId={}",
                    doctor.getEmail(), doctor.getUserId());

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
            updateKeycloakPassword(doctor.getUserId(), request.getNewPassword());

            log.info("========================================");
            log.info("✅ PASSWORD CHANGED SUCCESSFULLY IN KEYCLOAK");
            log.info("========================================");
            log.info("Doctor: {} ({})", doctor.getFullName(), doctor.getEmail());
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
     * Le doctor devra changer ce mot de passe à sa prochaine connexion
     */
    public void setTemporaryPassword(String doctorId, String temporaryPassword) {
        try {
            log.info("🔐 Setting temporary password for doctor: {}", doctorId);

            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            UserResource userResource = keycloak.realm(realm)
                    .users()
                    .get(doctor.getUserId());

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
    public void requirePasswordChange(String doctorId) {
        try {
            log.info("🔐 Requiring password change for doctor: {}", doctorId);

            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            UserResource userResource = keycloak.realm(realm)
                    .users()
                    .get(doctor.getUserId());

            UserRepresentation user = userResource.toRepresentation();
            user.getRequiredActions().add("UPDATE_PASSWORD");

            userResource.update(user);

            log.info("✅ Password change required successfully");

        } catch (Exception e) {
            log.error("❌ Failed to require password change", e);
            throw new RuntimeException("Failed to require password change: " + e.getMessage(), e);
        }
    }
}