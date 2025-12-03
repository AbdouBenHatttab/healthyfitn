package com.healthapp.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service d'envoi d'emails compatible avec Keycloak
 * Accepte maintenant les UserRepresentation de Keycloak
 */
@Service
@Slf4j
public class EmailService {

    /**
     * Notification aux admins lors de l'inscription d'un nouveau médecin
     */
    public void sendDoctorRegistrationNotificationToAdmin(UserRepresentation doctor) {
        Map<String, List<String>> attributes = doctor.getAttributes();

        log.info("📧 ========================================");
        log.info("📧 NEW DOCTOR REGISTRATION - ADMIN NOTIFICATION");
        log.info("📧 ========================================");
        log.info("📧 Doctor Details:");
        log.info("📧   Name: {} {}", doctor.getFirstName(), doctor.getLastName());
        log.info("📧   Email: {}", doctor.getEmail());
        log.info("📧   Keycloak ID: {}", doctor.getId());
        log.info("📧   License: {}", getAttributeValue(attributes, "medicalLicenseNumber"));
        log.info("📧   Specialization: {}", getAttributeValue(attributes, "specialization"));
        log.info("📧   Hospital: {}", getAttributeValue(attributes, "hospitalAffiliation"));
        log.info("📧   Experience: {} years", getAttributeValue(attributes, "yearsOfExperience"));
        log.info("📧 ========================================");
        log.info("📧 Action Required: Review and approve this doctor in Keycloak");
        log.info("📧 ========================================");
    }

    /**
     * Confirmation d'activation du compte médecin
     */
    public void sendDoctorActivationConfirmation(UserRepresentation doctor) {
        log.info("✅ ========================================");
        log.info("✅ DOCTOR ACCOUNT ACTIVATED");
        log.info("✅ ========================================");
        log.info("✅ Email sent to: {}", doctor.getEmail());
        log.info("✅ Name: {} {}", doctor.getFirstName(), doctor.getLastName());
        log.info("✅ Message: Your account has been activated!");
        log.info("✅ You can now login to the platform.");
        log.info("✅ ========================================");
    }

    /**
     * Notification de rejet du compte médecin
     */
    public void sendDoctorRejectionNotification(UserRepresentation doctor, String reason) {
        log.info("❌ ========================================");
        log.info("❌ DOCTOR ACCOUNT REJECTED");
        log.info("❌ ========================================");
        log.info("❌ Email sent to: {}", doctor.getEmail());
        log.info("❌ Name: {} {}", doctor.getFirstName(), doctor.getLastName());
        log.info("❌ Reason: {}", reason);
        log.info("❌ ========================================");
    }

    /**
     * Email de vérification d'adresse email
     */
    public void sendEmailVerification(UserRepresentation user, String verificationLink) {
        log.info("📧 ========================================");
        log.info("📧 EMAIL VERIFICATION");
        log.info("📧 ========================================");
        log.info("📧 Recipient: {}", user.getEmail());
        log.info("📧 Verification Link: {}", verificationLink);
        log.info("📧 ========================================");
    }

    /**
     * Email de réinitialisation de mot de passe
     */
    public void sendPasswordReset(UserRepresentation user, String resetLink) {
        log.info("🔐 ========================================");
        log.info("🔐 PASSWORD RESET REQUEST");
        log.info("🔐 ========================================");
        log.info("🔐 Recipient: {}", user.getEmail());
        log.info("🔐 Reset Link: {}", resetLink);
        log.info("🔐 ========================================");
    }

    /**
     * Helper: Extraire une valeur d'attribut de Keycloak
     */
    private String getAttributeValue(Map<String, List<String>> attributes, String key) {
        if (attributes == null || !attributes.containsKey(key)) {
            return "N/A";
        }
        List<String> values = attributes.get(key);
        return values.isEmpty() ? "N/A" : values.get(0);
    }
}