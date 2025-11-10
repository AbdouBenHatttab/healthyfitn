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
        log.info("Admin requesting pending doctors list");
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
        
        log.info("🔍 Admin processing doctor activation: {} - Action: {}", 
                request.getDoctorId(), request.getAction());
        
        // Récupérer l'Authentication depuis SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        log.info("🔍 Authentication object: {}", authentication);
        log.info("🔍 Is authenticated: {}", authentication != null ? authentication.isAuthenticated() : "null");
        
        if (authentication == null) {
            log.error("❌ Authentication is NULL in SecurityContext!");
            return ResponseEntity.status(401).body(Map.of(
                "status", "error",
                "message", "Authentication required - no auth found in context"
            ));
        }
        
        if (!authentication.isAuthenticated()) {
            log.error("❌ User is not authenticated!");
            return ResponseEntity.status(401).body(Map.of(
                "status", "error",
                "message", "Authentication required - user not authenticated"
            ));
        }
        
        // Extraire l'email de l'admin
        String adminEmail = authentication.getName();
        String adminId = adminEmail;
        
        log.info("✅ Admin authenticated: email={}, authorities={}", 
                adminEmail, authentication.getAuthorities());
        
        doctorActivationService.processDoctorActivation(request, adminId, adminEmail);
        
        String message = "APPROVE".equalsIgnoreCase(request.getAction())
                ? "Doctor account has been successfully activated"
                : "Doctor account activation has been rejected";
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", message
        ));
    }
    /**
     * ✅ NOUVEAU: Récupérer la liste des médecins activés
     */
    @GetMapping("/activated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DoctorPendingResponse>> getActivatedDoctors() {
        log.info("📋 Admin requesting activated doctors list");
        List<DoctorPendingResponse> activatedDoctors = doctorActivationService.getActivatedDoctors();
        log.info("✅ Found {} activated doctors", activatedDoctors.size());
        return ResponseEntity.ok(activatedDoctors);
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
}