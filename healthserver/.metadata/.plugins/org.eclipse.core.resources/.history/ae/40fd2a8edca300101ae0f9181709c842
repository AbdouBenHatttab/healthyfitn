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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AdminDoctorController - Endpoints pour les admins
 * 
 * Ces endpoints nécessitent le rôle ADMIN
 */
@RestController
@RequestMapping("/api/admin/doctors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminDoctorController {
    
    private final DoctorActivationService doctorActivationService;
    
    /**
     * Récupérer la liste des médecins en attente d'activation
     */
    @GetMapping("/pending")
    public ResponseEntity<List<DoctorPendingResponse>> getPendingDoctors() {
        log.info("Admin requesting pending doctors list");
        
        List<DoctorPendingResponse> pendingDoctors = doctorActivationService.getPendingDoctors();
        
        return ResponseEntity.ok(pendingDoctors);
    }
    
    /**
     * Approuver ou rejeter un médecin
     */
    @PostMapping("/activate")
    public ResponseEntity<Map<String, String>> activateDoctor(
            @Valid @RequestBody DoctorActivationRequestDto request,
            Authentication authentication) {
        
        log.info("Admin processing doctor activation: {} - Action: {}", 
                request.getDoctorId(), request.getAction());
        
        // Extraire les infos de l'admin depuis le JWT
        String adminId = authentication.getName(); // ou extraire depuis le JWT
        String adminEmail = authentication.getName(); // Email de l'admin
        
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
     * Compter les médecins en attente
     */
    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Long>> getPendingDoctorsCount() {
        long count = doctorActivationService.getPendingDoctorsCount();
        
        return ResponseEntity.ok(Map.of("count", count));
    }
}