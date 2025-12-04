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

/**
 * Service de changement de mot de passe avec Keycloak
 *
 * ✅ CHANGEMENT MAJEUR:
 * Le changement de mot de passe est maintenant géré par Keycloak.
 * Ce service met à jour le mot de passe dans Keycloak uniquement.
 *
 * ⚠️ IMPORTANT:
 * MongoDB ne stocke PLUS de mot de passe (champ password = null)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorPasswordService {

    private final DoctorRepository doctorRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    /**
     * Changer le mot de passe dans Keycloak
     *
     * ⚠️ LIMITATION:
     * On ne peut pas vérifier l'ancien mot de passe via l'API Admin Keycloak.
     * Pour une vraie vérification, le frontend devrait utiliser l'API OAuth2 de Keycloak.
     *
     * SOLUTION RECOMMANDÉE:
     * Le frontend devrait rediriger vers la page de changement de mot de passe Keycloak:
     * http://localhost:8080/realms/health-app-realm/account/password
     */
    public void changePassword(String doctorId, ChangePasswordRequest request) {
        log.info("========================================");
        log.info("🔐 PASSWORD CHANGE REQUEST (KEYCLOAK)");
        log.info("========================================");
        log.info("Doctor ID: {}", doctorId);

        // Validate request
        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            log.error("❌ New password is null or empty");
            throw new IllegalArgumentException("New password is required");
        }

        try {
            // Find doctor in MongoDB
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> {
                        log.error("❌ Doctor not found with id: {}", doctorId);
                        return new RuntimeException("Doctor not found with id: " + doctorId);
                    });

            log.info("✅ Doctor found: email={}, keycloakId={}",
                    doctor.getEmail(), doctor.getUserId());

            // ⚠️ AVERTISSEMENT:
            // On ne peut pas vérifier l'ancien mot de passe via l'API Admin Keycloak.
            // Cette vérification devrait être faite côté frontend via OAuth2.

            if (request.getCurrentPassword() != null && !request.getCurrentPassword().isEmpty()) {
                log.warn("⚠️ Current password verification is not supported with Keycloak Admin API");
                log.warn("⚠️ Skipping current password check");
                log.warn("⚠️ RECOMMENDATION: Use Keycloak Account Console for password change");
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

            if (!user.isEnabled()) {
                throw new RuntimeException("User account is disabled");
            }

            // Créer la représentation du nouveau mot de passe
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false); // Mot de passe permanent

            // ✅ Mettre à jour le mot de passe
            userResource.resetPassword(credential);

            log.info("✅ Password updated successfully in Keycloak");

        } catch (Exception e) {
            log.error("❌ Failed to update password in Keycloak", e);
            log.error("   Keycloak User ID: {}", keycloakUserId);
            log.error("   Error: {}", e.getMessage());

            throw new RuntimeException("Failed to update Keycloak password: " + e.getMessage(), e);
        }
    }

    /**
     * ⚠️ MÉTHODE ALTERNATIVE RECOMMANDÉE
     *
     * Pour un changement de mot de passe sécurisé avec vérification de l'ancien mot de passe,
     * redirigez l'utilisateur vers la console Keycloak Account:
     *
     * http://localhost:8080/realms/health-app-realm/account/password
     * http://localhost:8080/realms/health-app-realm/account/password
     *
     * Ou utilisez l'API OAuth2 de Keycloak côté frontend.
     */
    public String getKeycloakPasswordChangeUrl() {
        return String.format(
                "http://localhost:8080/realms/%s/account/password",
                realm
        );
    }
}