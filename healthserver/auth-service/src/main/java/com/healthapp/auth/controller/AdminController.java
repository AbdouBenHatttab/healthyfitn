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
 * Admin Controller - Manage doctor activations
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final AdminService adminService;
    
    /**
     * Get all pending doctors (waiting for activation)
     */
    @GetMapping("/doctors/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getPendingDoctors() {
        log.info("Admin requesting pending doctors");
        List<UserResponse> pendingDoctors = adminService.getPendingDoctors();
        return ResponseEntity.ok(pendingDoctors);
    }
    
    /**
     * Activate a doctor account
     */
    @PostMapping("/doctors/{doctorId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> activateDoctor(@PathVariable String doctorId) {
        log.info("Admin activating doctor: {}", doctorId);
        adminService.activateDoctor(doctorId);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Doctor account activated successfully"
        ));
    }
    
    /**
     * Reject a doctor account
     */
    @PostMapping("/doctors/{doctorId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> rejectDoctor(
            @PathVariable String doctorId,
            @RequestBody(required = false) Map<String, String> body) {
        
        String reason = body != null ? body.get("reason") : "Credentials could not be verified";
        log.info("Admin rejecting doctor: {} - Reason: {}", doctorId, reason);
        
        adminService.rejectDoctor(doctorId, reason);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Doctor account rejected"
        ));
    }
    
    /**
     * Get count of pending doctors
     */
    @GetMapping("/doctors/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getPendingDoctorsCount() {
        long count = adminService.getPendingDoctorsCount();
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    /**
     * Get all activated doctors
     */
    @GetMapping("/doctors/activated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getActivatedDoctors() {
        log.info("Admin requesting activated doctors");
        List<UserResponse> activatedDoctors = adminService.getActivatedDoctors();
        return ResponseEntity.ok(activatedDoctors);
    }
}