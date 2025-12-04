package com.healthapp.doctor.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import com.healthapp.shared.enums.Gender;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Doctor Entity - Informations spécifiques aux médecins avec Keycloak
 *
 * ⚠️ NOTE: Le password est géré par Keycloak, pas stocké dans MongoDB
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "doctors")
public class Doctor {

    @Id
    private String id;

    // Référence vers l'utilisateur dans Keycloak
    @Indexed(unique = true)
    private String keycloakUserId; // ✅ NOUVEAU: ID de l'utilisateur dans Keycloak

    // ID interne optionnel (peut être utilisé pour les références)
    @Indexed(unique = true)
    private String userId;

    @Indexed(unique = true)
    private String email;

    // ✅ Email réel pour notifications (Gmail, Outlook, etc.)
    @Indexed
    private String contactEmail;

    // ⚠️ PAS DE PASSWORD - Géré par Keycloak
    private String password; // SUPPRIMÉ

    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String profilePictureUrl;

    // Informations médicales
    @Indexed(unique = true)
    private String medicalLicenseNumber;

    private String specialization;
    private String hospitalAffiliation;
    private Integer yearsOfExperience;

    // Informations de contact
    private String phoneNumber;
    private Gender gender;
    private String officeAddress;
    private String consultationHours;

    // Documents et certifications
    private String licenseDocumentUrl;
    private String diplomaDocumentUrl;
    private String[] certifications;

    // Statut d'activation
    @Builder.Default
    private Boolean isActivated = false;

    private String activationStatus; // PENDING, APPROVED, REJECTED

    @CreatedDate
    private LocalDateTime activationRequestDate;

    private LocalDateTime activationDate;
    private String activatedBy; // Admin ID

    private LocalDateTime rejectionDate;
    private String rejectedBy; // Admin ID
    private String rejectionReason;

    // Statistiques (pour futur)
    private Integer totalPatients;
    private Double averageRating;
    private Integer totalConsultations;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Méthodes business
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isPending() {
        return "PENDING".equals(activationStatus);
    }

    /**
     * Retourne l'email à utiliser pour les notifications
     * Priorité: contactEmail > email
     */
    public String getNotificationEmail() {
        return contactEmail != null && !contactEmail.isEmpty()
                ? contactEmail
                : email;
    }

    /**
     * ✅ NOUVEAU: Vérifie si le médecin est authentifié via Keycloak
     */
    public boolean isKeycloakAuthenticated() {
        return keycloakUserId != null && !keycloakUserId.isEmpty();
    }
}