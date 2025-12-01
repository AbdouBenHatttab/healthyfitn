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
 * Contrôleur pour les médecins authentifiés
 * Fournit les endpoints pour gérer le profil, mot de passe et emails
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Slf4j
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final DoctorPasswordService doctorPasswordService;
    private final DoctorPasswordResetService passwordResetService;

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("✅ DoctorController INITIALISÉ");
        log.info("✅ Chemin de base : /api/doctors");
        log.info("========================================");
    }

    /**
     * ENDPOINT DE TEST
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        log.info("🧪 Endpoint TEST appelé avec succès !");
        return ResponseEntity.ok(Map.of(
                "statut", "OK",
                "message", "DoctorController fonctionne correctement !",
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
                        "isActivated", d.getIsActivated()
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
     * Changer le mot de passe du médecin
     */
    @PutMapping("/change-password")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> changeDoctorPassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        log.info("🔐 [MOT DE PASSE] Endpoint appelé par : {}", authentication.getName());

        try {
            String email = authentication.getName();

            if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
                log.error("❌ Mot de passe actuel manquant");
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Le mot de passe actuel est requis"));
            }

            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                log.error("❌ Nouveau mot de passe manquant");
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Le nouveau mot de passe est requis"));
            }

            Doctor doctor = doctorRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Médecin non trouvé pour l'email : " + email));

            doctorPasswordService.changePassword(doctor.getId(), request);

            log.info("✅ Mot de passe changé avec succès !");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mot de passe changé avec succès"
            ));

        } catch (RuntimeException e) {
            log.error("❌ Erreur mot de passe : {}", e.getMessage());

            if (e.getMessage().contains("Current password is incorrect")) {
                return ResponseEntity.status(401)
                        .body(Map.of("success", false, "error", "Le mot de passe actuel est incorrect"));
            }

            if (e.getMessage().contains("New password must be different")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Le nouveau mot de passe doit être différent de l'actuel"));
            }

            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Mot de passe oublié - version sécurisée
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotDoctorPassword(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("L'email est requis");
        }

        log.info("🔐 Réinitialisation de mot de passe demandée pour le médecin : {}", email);

        try {
            passwordResetService.sendPasswordResetEmailForDoctor(email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email de réinitialisation envoyé avec succès"
            ));

        } catch (Exception e) {
            log.error("❌ Échec de l'envoi de l'email de réinitialisation : {}", e.getMessage());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Si l'email existe, un lien de réinitialisation sera envoyé"
            ));
        }
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
