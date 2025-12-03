package com.healthapp.auth.service;

import com.healthapp.auth.dto.response.UserResponse;
import com.healthapp.auth.Enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service Administrateur avec Keycloak
 * Gestion de l'activation et du rejet des médecins
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final KeycloakAdminService keycloakAdminService;
    private final EmailService emailService;

    /**
     * Récupérer tous les médecins en attente d'activation
     */
    public List<UserResponse> getPendingDoctors() {
        log.info("📋 Récupération des médecins en attente depuis Keycloak");

        List<UserRepresentation> pendingDoctors = keycloakAdminService.getPendingDoctors();

        log.info("📋 {} médecin(s) en attente trouvés", pendingDoctors.size());

        return pendingDoctors.stream()
                .map(this::mapKeycloakUserToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Activer un compte médecin dans Keycloak
     */
    public void activateDoctor(String doctorId) {
        log.info("✅ Activation du médecin : {}", doctorId);

        // Activer le médecin dans Keycloak
        keycloakAdminService.activateDoctor(doctorId);

        // Récupérer les infos du médecin pour l'email
        keycloakAdminService.getUserByEmail("email").ifPresent(doctor -> {
            // Envoyer un email de confirmation
            emailService.sendDoctorActivationConfirmation(doctor);
        });

        log.info("✅ Médecin activé avec succès dans Keycloak");
    }

    /**
     * Rejeter un compte médecin
     * Note : Dans Keycloak, on peut simplement supprimer l'utilisateur
     * ou désactiver définitivement le compte
     */
    public void rejectDoctor(String doctorId, String reason) {
        log.info("❌ Rejet du médecin : {} - Raison : {}", doctorId, reason);

        // TODO: Selon vos besoins, vous pouvez :
        // 1. Supprimer complètement l'utilisateur :
        //    keycloakAdminService.deleteUser(doctorId);
        //
        // 2. Ou marquer le compte comme rejeté dans les attributs :
        //    keycloakAdminService.addAttribute(doctorId, "rejectionReason", reason);

        // Pour le moment, on envoie juste l'email de rejet
        keycloakAdminService.getUserByEmail("email").ifPresent(doctor -> {
            emailService.sendDoctorRejectionNotification(doctor, reason);
        });

        log.info("❌ Médecin rejeté");
    }

    /**
     * Obtenir le nombre de médecins en attente
     */
    public long getPendingDoctorsCount() {
        return keycloakAdminService.getPendingDoctors().size();
    }

    /**
     * Récupérer tous les médecins activés
     * Note : Dans Keycloak, ce sont les utilisateurs avec rôle DOCTOR et enabled=true
     */
    public List<UserResponse> getActivatedDoctors() {
        log.info("📋 Récupération des médecins activés depuis Keycloak");

        // TODO: Implémenter dans KeycloakAdminService une méthode
        // pour récupérer tous les utilisateurs avec rôle DOCTOR et enabled=true

        // Pour le moment, retourner une liste vide
        // Vous devrez ajouter cette méthode dans KeycloakAdminService

        return List.of();
    }

    /**
     * Mapper un UserRepresentation Keycloak vers UserResponse
     */
    private UserResponse mapKeycloakUserToResponse(UserRepresentation keycloakUser) {
        Map<String, List<String>> attributes = keycloakUser.getAttributes();

        return UserResponse.builder()
                .id(keycloakUser.getId())
                .email(keycloakUser.getEmail())
                .firstName(keycloakUser.getFirstName())
                .lastName(keycloakUser.getLastName())
                .fullName(keycloakUser.getFirstName() + " " + keycloakUser.getLastName())
                .roles(Set.of(UserRole.DOCTOR))  // On sait que ce sont des médecins
                .isActivated(keycloakUser.isEnabled())
                .isEmailVerified(keycloakUser.isEmailVerified())
                .medicalLicenseNumber(getAttributeValue(attributes, "medicalLicenseNumber"))
                .specialization(getAttributeValue(attributes, "specialization"))
                .hospitalAffiliation(getAttributeValue(attributes, "hospitalAffiliation"))
                .yearsOfExperience(getIntAttributeValue(attributes, "yearsOfExperience"))
                .build();
    }

    /**
     * Extraire une valeur d'attribut string de Keycloak
     */
    private String getAttributeValue(Map<String, List<String>> attributes, String key) {
        if (attributes == null || !attributes.containsKey(key)) {
            return null;
        }
        List<String> values = attributes.get(key);
        return values.isEmpty() ? null : values.get(0);
    }

    /**
     * Extraire une valeur d'attribut integer de Keycloak
     */
    private Integer getIntAttributeValue(Map<String, List<String>> attributes, String key) {
        String value = getAttributeValue(attributes, key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}