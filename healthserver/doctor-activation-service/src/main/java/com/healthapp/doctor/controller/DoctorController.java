package com.healthapp.doctor.controller;

import com.healthapp.doctor.dto.request.UpdateDoctorProfileRequest;
import com.healthapp.doctor.dto.response.DoctorResponse;
import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.repository.DoctorRepository;
import com.healthapp.doctor.service.DoctorPasswordResetService;
import com.healthapp.doctor.service.DoctorPasswordService;
import com.healthapp.doctor.dto.request.ChangePasswordRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur pour les médecins authentifiés avec Keycloak
 *
 * ✅ CHANGEMENTS AVEC KEYCLOAK:
 * - /forgot-password : Déclenche l'action Keycloak
 * - /change-password : Met à jour dans Keycloak (avec limitations)
 *
 * ⚠️ RECOMMANDATION:
 * Pour un changement de mot de passe complet, redirigez vers:
 * http://localhost:8080/realms/health-app-realm/account/password
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Slf4j
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final DoctorPasswordService doctorPasswordService;
    private final DoctorPasswordResetService passwordResetService;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("✅ DoctorController INITIALIZED (KEYCLOAK)");
        log.info("✅ Base path: /api/doctors");
        log.info("========================================");
    }

    /**
     * ENDPOINT DE TEST
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        log.info("🧪 Endpoint TEST appelé avec succès !");
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "DoctorController fonctionne correctement avec Keycloak !",
                "authentication", "Keycloak OAuth2",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    /**
     * ENDPOINT DEBUG - Affiche tous les emails des médecins
     */
    @GetMapping("/debug/all-emails")
    public ResponseEntity<Map<String, Object>> getAllEmails() {
        List<Doctor> allDoctors = doctorRepository.findAll();

        Map<String, Object> debug = new HashMap<>();
        debug.put("totalDoctors", allDoctors.size());
        debug.put("emails", allDoctors.stream()
                .map(d -> Map.of(
                        "email", d.getEmail(),
                        "contactEmail", d.getContactEmail() != null ? d.getContactEmail() : "N/A",
                        "userId", d.getUserId(),
                        "isActivated", d.getIsActivated(),
                        "hasPassword", d.getPassword() != null ? "YES (legacy)" : "NO (Keycloak)"
                ))
                .collect(Collectors.toList()));

        return ResponseEntity.ok(debug);
    }

    /**
     * Récupérer le profil du médecin authentifié
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorResponse> getDoctorProfile(Authentication authentication) {
        String email = authentication.getName();
        log.info("🔍 [PROFIL] Recherche du profil du médecin pour l'email : '{}'", email);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé pour l'email : " + email));

        log.info("✅ [PROFIL] Médecin trouvé : id={}, email='{}', contactEmail='{}'",
                doctor.getId(), doctor.getEmail(), doctor.getContactEmail());

        return ResponseEntity.ok(mapToDoctorResponse(doctor));
    }

    /**
     * Mettre à jour le profil du médecin
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorResponse> updateDoctorProfile(
            @RequestBody UpdateDoctorProfileRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        log.info("🔄 [MISE À JOUR] Mise à jour du profil pour l'email : '{}'", email);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé pour l'email : " + email));

        if (request.getFirstName() != null) doctor.setFirstName(request.getFirstName());
        if (request.getLastName() != null) doctor.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) doctor.setPhoneNumber(request.getPhoneNumber());
        if (request.getContactEmail() != null) {
            log.info("📧 Mise à jour de l'email de contact : {}", request.getContactEmail());
            doctor.setContactEmail(request.getContactEmail());
        }
        if (request.getSpecialization() != null) doctor.setSpecialization(request.getSpecialization());
        if (request.getHospitalAffiliation() != null) doctor.setHospitalAffiliation(request.getHospitalAffiliation());
        if (request.getYearsOfExperience() != null) doctor.setYearsOfExperience(request.getYearsOfExperience());
        if (request.getOfficeAddress() != null) doctor.setOfficeAddress(request.getOfficeAddress());
        if (request.getConsultationHours() != null) doctor.setConsultationHours(request.getConsultationHours());
        if (request.getProfilePictureUrl() != null) doctor.setProfilePictureUrl(request.getProfilePictureUrl());

        Doctor updatedDoctor = doctorRepository.save(doctor);
        log.info("✅ [MISE À JOUR] Profil du médecin mis à jour : {}", doctor.getEmail());

        return ResponseEntity.ok(mapToDoctorResponse(updatedDoctor));
    }

    /**
     * ⚠️ CHANGEMENT DE MOT DE PASSE AVEC KEYCLOAK
     *
     * LIMITATIONS:
     * - Impossible de vérifier l'ancien mot de passe via Admin API
     * - Le mot de passe est mis à jour directement dans Keycloak
     *
     * RECOMMANDATION:
     * Utilisez plutôt l'endpoint /password-change-url et redirigez vers Keycloak
     */
    @PutMapping("/change-password")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> changeDoctorPassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        log.info("========================================");
        log.info("🔐 PASSWORD CHANGE REQUEST (KEYCLOAK)");
        log.info("========================================");
        log.info("User: {}", authentication.getName());

        log.warn("⚠️ LIMITATION: Current password verification not available with Keycloak Admin API");
        log.warn("⚠️ RECOMMENDATION: Use Keycloak Account Console for secure password change");

        try {
            String email = authentication.getName();

            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                log.error("❌ Nouveau mot de passe manquant");
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "error", "Le nouveau mot de passe est requis"
                        ));
            }

            Doctor doctor = doctorRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Médecin non trouvé pour l'email : " + email));

            // ⚠️ Le service changera le mot de passe dans Keycloak
            // mais ne pourra pas vérifier l'ancien mot de passe
            doctorPasswordService.changePassword(doctor.getId(), request);

            log.info("✅ Mot de passe changé avec succès dans Keycloak !");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mot de passe changé avec succès",
                    "note", "Password updated in Keycloak"
            ));

        } catch (RuntimeException e) {
            log.error("❌ Erreur mot de passe : {}", e.getMessage());

            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * ✅ NOUVEAU: Obtenir l'URL de changement de mot de passe Keycloak
     *
     * RECOMMANDÉ: Redirigez l'utilisateur vers cette URL pour un changement
     * de mot de passe sécurisé avec vérification de l'ancien mot de passe.
     */
    @GetMapping("/password-change-url")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, String>> getPasswordChangeUrl() {
        String url = String.format(
                "%s/realms/%s/account/password",
                keycloakServerUrl,
                keycloakRealm
        );

        return ResponseEntity.ok(Map.of(
                "url", url,
                "message", "Redirect user to this URL for secure password change",
                "note", "Keycloak will handle old password verification"
        ));
    }

    /**
     * Mot de passe oublié - Déclenche l'action Keycloak
     *
     * ✅ AVEC KEYCLOAK:
     * - Keycloak envoie automatiquement l'email de réinitialisation
     * - Pas besoin de gérer les tokens manuellement
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotDoctorPassword(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("L'email est requis");
        }

        log.info("========================================");
        log.info("🔐 PASSWORD RESET REQUEST (KEYCLOAK)");
        log.info("========================================");
        log.info("Email: {}", email);

        try {
            passwordResetService.sendPasswordResetEmailForDoctor(email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Si l'email existe, un lien de réinitialisation sera envoyé par Keycloak",
                    "provider", "Keycloak"
            ));

        } catch (Exception e) {
            log.error("❌ Échec de l'envoi de l'email de réinitialisation : {}", e.getMessage());

            // Ne pas révéler si l'email existe ou non (sécurité)
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Si l'email existe, un lien de réinitialisation sera envoyé"
            ));
        }
    }

    /**
     * ✅ NOUVEAU: Obtenir l'URL de réinitialisation de mot de passe Keycloak
     */
    @GetMapping("/password-reset-url")
    public ResponseEntity<Map<String, String>> getPasswordResetUrl() {
        String url = String.format(
                "%s/realms/%s/login-actions/reset-credentials",
                keycloakServerUrl,
                keycloakRealm
        );

        return ResponseEntity.ok(Map.of(
                "url", url,
                "message", "Redirect user to this URL for password reset",
                "note", "User will receive an email from Keycloak"
        ));
    }

    /**
     * Convertir un Doctor en DoctorResponse
     */
    private DoctorResponse mapToDoctorResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .userId(doctor.getUserId())
                .email(doctor.getEmail())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .fullName(doctor.getFullName())
                .phoneNumber(doctor.getPhoneNumber())
                .medicalLicenseNumber(doctor.getMedicalLicenseNumber())
                .specialization(doctor.getSpecialization())
                .hospitalAffiliation(doctor.getHospitalAffiliation())
                .yearsOfExperience(doctor.getYearsOfExperience())
                .officeAddress(doctor.getOfficeAddress())
                .consultationHours(doctor.getConsultationHours())
                .isActivated(doctor.getIsActivated())
                .activationStatus(doctor.getActivationStatus())
                .activationDate(doctor.getActivationDate())
                .activationRequestDate(doctor.getActivationRequestDate())
                .totalPatients(doctor.getTotalPatients())
                .averageRating(doctor.getAverageRating())
                .totalConsultations(doctor.getTotalConsultations())
                .createdAt(doctor.getCreatedAt())
                .profilePictureUrl(doctor.getProfilePictureUrl())
                .build();
    }
}