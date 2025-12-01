package com.healthapp.auth.controller;

import com.healthapp.auth.dto.response.UserResponse;
import com.healthapp.auth.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur Admin - Gestion des comptes médecins
 * Tous les endpoints nécessitent le rôle ADMIN
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    /**
     * Récupérer tous les médecins en attente d'activation
     */
    @GetMapping("/doctors/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getPendingDoctors() {
        log.info("👨‍⚕️ Admin demande la liste des médecins en attente");
        List<UserResponse> pendingDoctors = adminService.getPendingDoctors();
        return ResponseEntity.ok(pendingDoctors);
    }

    /**
     * Activer un compte médecin
     */
    @PostMapping("/doctors/{doctorId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> activateDoctor(@PathVariable String doctorId) {
        log.info("✅ Admin active le médecin: {}", doctorId);
        adminService.activateDoctor(doctorId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Le compte médecin a été activé avec succès"
        ));
    }

    /**
     * Rejeter un compte médecin avec raison optionnelle
     */
    @PostMapping("/doctors/{doctorId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> rejectDoctor(
            @PathVariable String doctorId,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = body != null ? body.get("reason") : "Les informations n'ont pas pu être vérifiées";
        log.info("❌ Admin rejette le médecin: {} - Raison: {}", doctorId, reason);

        adminService.rejectDoctor(doctorId, reason);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Le compte médecin a été rejeté"
        ));
    }

    /**
     * Obtenir le nombre de médecins en attente
     */
    @GetMapping("/doctors/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getPendingDoctorsCount() {
        long count = adminService.getPendingDoctorsCount();
        log.info("📊 Nombre de médecins en attente: {}", count);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Récupérer tous les médecins activés
     */
    @GetMapping("/doctors/activated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getActivatedDoctors() {
        log.info("👨‍⚕️ Admin demande la liste des médecins activés");
        List<UserResponse> activatedDoctors = adminService.getActivatedDoctors();
        return ResponseEntity.ok(activatedDoctors);
    }
}
