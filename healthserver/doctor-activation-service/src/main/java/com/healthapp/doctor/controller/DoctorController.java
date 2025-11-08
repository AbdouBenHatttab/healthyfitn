package com.healthapp.doctor.controller;

import com.healthapp.doctor.dto.request.UpdateDoctorProfileRequest;
import com.healthapp.doctor.dto.response.DoctorResponse;
import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.repository.DoctorRepository;
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
 * DoctorController - Endpoints for authenticated doctors
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Slf4j
public class DoctorController {
    
    private final DoctorRepository doctorRepository;
    private final DoctorPasswordService doctorPasswordService;
    
    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("✅ DoctorController INITIALIZED");
        log.info("✅ Base path: /api/doctors");
        log.info("========================================");
    }
    
    /**
     * TEST ENDPOINT
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        log.info("🧪 TEST endpoint called successfully!");
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "message", "DoctorController is working!",
            "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
    
    /**
     * DEBUG ENDPOINT - Check database state
     */
    @GetMapping("/debug/all-emails")
    public ResponseEntity<Map<String, Object>> getAllEmails() {
        List<Doctor> allDoctors = doctorRepository.findAll();
        
        Map<String, Object> debug = new HashMap<>();
        debug.put("totalDoctors", allDoctors.size());
        debug.put("emails", allDoctors.stream()
            .map(d -> Map.of(
                "email", d.getEmail(),
                "contactEmail", d.getContactEmail() != null ? d.getContactEmail() : "N/A", // ✅ NOUVEAU
                "userId", d.getUserId(),
                "isActivated", d.getIsActivated()
            ))
            .collect(Collectors.toList()));
        
        return ResponseEntity.ok(debug);
    }
    
    /**
     * Get authenticated doctor's profile
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorResponse> getDoctorProfile(Authentication authentication) {
        String email = authentication.getName();
        log.info("🔍 [PROFILE] Looking up doctor profile for email: '{}'", email);
        
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Doctor not found with email: " + email));
        
        log.info("✅ [PROFILE] Doctor found: id={}, email='{}', contactEmail='{}'", 
                doctor.getId(), doctor.getEmail(), doctor.getContactEmail());
        
        return ResponseEntity.ok(mapToDoctorResponse(doctor));
    }
    
    /**
     * Update doctor profile - ✅ Inclut contactEmail
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorResponse> updateDoctorProfile(
            @RequestBody UpdateDoctorProfileRequest request,
            Authentication authentication) {
        
        String email = authentication.getName();
        log.info("🔄 [UPDATE] Updating profile for email: '{}'", email);
        
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Doctor not found with email: " + email));
        
        if (request.getFirstName() != null) doctor.setFirstName(request.getFirstName());
        if (request.getLastName() != null) doctor.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) doctor.setPhoneNumber(request.getPhoneNumber());
        
        // ✅ NOUVEAU: Permettre de modifier le contact email
        if (request.getContactEmail() != null) {
            log.info("📧 Updating contact email to: {}", request.getContactEmail());
            doctor.setContactEmail(request.getContactEmail());
        }
        
        if (request.getSpecialization() != null) doctor.setSpecialization(request.getSpecialization());
        if (request.getHospitalAffiliation() != null) doctor.setHospitalAffiliation(request.getHospitalAffiliation());
        if (request.getYearsOfExperience() != null) doctor.setYearsOfExperience(request.getYearsOfExperience());
        if (request.getOfficeAddress() != null) doctor.setOfficeAddress(request.getOfficeAddress());
        if (request.getConsultationHours() != null) doctor.setConsultationHours(request.getConsultationHours());
        if (request.getProfilePictureUrl() != null) doctor.setProfilePictureUrl(request.getProfilePictureUrl());
        
        Doctor updatedDoctor = doctorRepository.save(doctor);
        log.info("✅ [UPDATE] Doctor profile updated: {}", doctor.getEmail());
        
        return ResponseEntity.ok(mapToDoctorResponse(updatedDoctor));
    }
    
    /**
     * Change doctor password
     */
    @PutMapping("/change-password")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> changeDoctorPassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        
        log.info("🔐🔐🔐 [PASSWORD] ENDPOINT CALLED! 🔐🔐🔐");
        log.info("🔍 [PASSWORD] Request received from: {}", authentication.getName());
        
        try {
            String email = authentication.getName();
            
            // Validation
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
                log.error("❌ [PASSWORD] Current password is missing");
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Current password is required"));
            }
            
            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                log.error("❌ [PASSWORD] New password is missing");
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "New password is required"));
            }
            
            // Find doctor
            Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("❌ [PASSWORD] Doctor not found for email: '{}'", email);
                    return new RuntimeException("Doctor not found with email: " + email);
                });
            
            log.info("✅ [PASSWORD] Doctor found: id={}, email={}", doctor.getId(), doctor.getEmail());
            
            // Change password
            doctorPasswordService.changePassword(doctor.getId(), request);
            
            log.info("✅✅✅ [PASSWORD] Password changed successfully! ✅✅✅");
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Password changed successfully"
            ));
            
        } catch (RuntimeException e) {
            log.error("❌ [PASSWORD] Error: {}", e.getMessage(), e);
            
            if (e.getMessage().contains("Current password is incorrect")) {
                return ResponseEntity.status(401)
                    .body(Map.of("success", false, "error", "Current password is incorrect"));
            }
            
            if (e.getMessage().contains("New password must be different")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "New password must be different from current password"));
            }
            
            return ResponseEntity.status(500)
                .body(Map.of("success", false, "error", e.getMessage()));
                
        } catch (Exception e) {
            log.error("❌ [PASSWORD] Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("success", false, "error", "An unexpected error occurred"));
        }
    }
    
    /**
     * Forgot password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotDoctorPassword(
            @RequestBody Map<String, String> request) {
        
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Doctor not found with email: " + email));
        
        log.info("📧 [FORGOT] Password reset requested for doctor: {}", email);
        return ResponseEntity.ok(Map.of("success", true, "message", "Reset link sent to email"));
    }
    
    /**
     * Check activation status
     */
    @GetMapping("/activation-status")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> getActivationStatus(Authentication authentication) {
        String email = authentication.getName();
        log.info("📊 [STATUS] Checking activation status for: '{}'", email);
        
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Doctor not found with email: " + email));
        
        String message = doctor.getIsActivated()
                ? "Your account is activated and ready to use"
                : "Your account is pending admin approval.";
        
        return ResponseEntity.ok(Map.of(
            "isActivated", doctor.getIsActivated(),
            "activationStatus", doctor.getActivationStatus(),
            "message", message,
            "activationRequestDate", doctor.getActivationRequestDate(),
            "activationDate", doctor.getActivationDate() != null ? doctor.getActivationDate() : "Not activated yet"
        ));
    }
    
    /**
     * Map Doctor to DoctorResponse
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