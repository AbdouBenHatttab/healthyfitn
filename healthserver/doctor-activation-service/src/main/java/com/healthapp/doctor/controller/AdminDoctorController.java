package com.healthapp.doctor.controller;

import com.healthapp.doctor.dto.request.DoctorActivationRequestDto;
import com.healthapp.doctor.dto.response.DoctorPendingResponse;
import com.healthapp.doctor.service.DoctorActivationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AdminDoctorController - Endpoints pour les admins
 */
@RestController
@RequestMapping("/api/admin/doctors")
@RequiredArgsConstructor
@Slf4j
public class AdminDoctorController {

    private final DoctorActivationService doctorActivationService;

    /**
     * Récupérer la liste des médecins en attente d'activation
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DoctorPendingResponse>> getPendingDoctors() {
        log.info("Admin demande la liste des médecins en attente");
        List<DoctorPendingResponse> pendingDoctors = doctorActivationService.getPendingDoctors();
        return ResponseEntity.ok(pendingDoctors);
    }

    /**
     * Approuver ou rejeter un médecin
     */
    @PostMapping("/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> activateDoctor(
            @Valid @RequestBody DoctorActivationRequestDto request) {

        log.info("🔍 Admin traite l'activation du médecin : {} - Action : {}",
                request.getDoctorId(), request.getAction());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("❌ Aucune authentification trouvée dans le contexte !");
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Authentification requise - aucun utilisateur trouvé"
            ));
        }

        if (!authentication.isAuthenticated()) {
            log.error("❌ L'utilisateur n'est pas authentifié !");
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Authentification requise - utilisateur non authentifié"
            ));
        }

        // Extraire l'email de l'admin
        String adminEmail = authentication.getName();
        String adminId = adminEmail;

        log.info("✅ Admin authentifié : email={}, authorities={}",
                adminEmail, authentication.getAuthorities());

        doctorActivationService.processDoctorActivation(request, adminId, adminEmail);

        String message = "APPROVE".equalsIgnoreCase(request.getAction())
                ? "Le compte du médecin a été activé avec succès"
                : "L'activation du compte du médecin a été rejetée";

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", message
        ));
    }

    /**
     * ✅ Récupérer la liste des médecins activés
     */
    @GetMapping("/activated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DoctorPendingResponse>> getActivatedDoctors() {
        log.info("📋 Admin demande la liste des médecins activés");
        List<DoctorPendingResponse> activatedDoctors = doctorActivationService.getActivatedDoctors();
        log.info("✅ {} médecins activés trouvés", activatedDoctors.size());
        return ResponseEntity.ok(activatedDoctors);
    }
    /**
     * ✅ Récupérer la liste des médecins
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DoctorPendingResponse>> getDoctors() {
        log.info("📋 Admin demande la liste des médecins activés");
        List<DoctorPendingResponse> Doctors = doctorActivationService.getDoctors();
        log.info("✅ {} médecins activés trouvés", Doctors.size());
        return ResponseEntity.ok(Doctors);
    }
    /**
     * Compter les médecins activés
     */
    @GetMapping("/activated/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getActivatedDoctorsCount() {
        long count = doctorActivationService.getActivatedDoctorsCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Compter les médecins en attente
     */
    @GetMapping("/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getPendingDoctorsCount() {
        long count = doctorActivationService.getPendingDoctorsCount();
        return ResponseEntity.ok(Map.of("count", count));
    }
    /**
     * Compter les médecins
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getDoctorsCount() {
        long count = doctorActivationService.getDoctorsCount();
        return ResponseEntity.ok(Map.of("count", count));
    }
    /**
     * Supprimer un médecin (seulement si APPROVED)
     */
    @DeleteMapping("/{doctorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteDoctor(@PathVariable String doctorId) {
        log.info("🗑️ Admin demande la suppression du médecin : {}", doctorId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("❌ Authentification requise");
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Authentification requise"
            ));
        }

        String adminId = authentication.getName();
        log.info("✅ Admin authentifié : {}", adminId);

        try {
            doctorActivationService.deleteDoctor(doctorId, adminId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Le médecin a été supprimé avec succès"
            ));
        } catch (IllegalStateException e) {
            log.error("❌ Action non autorisée : {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la suppression du médecin", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Erreur lors de la suppression du médecin"
            ));
        }
    }
}
