package com.healthapp.doctor.controller;

import com.healthapp.doctor.dto.request.UpdateDoctorProfileRequest;
import com.healthapp.doctor.dto.response.DoctorResponse;
import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DoctorController - Endpoints for authenticated doctors
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
@Slf4j
public class DoctorController {
    
    private final DoctorRepository doctorRepository;
    
    /**
     * DEBUG ENDPOINT - Check database state
     * Remove this in production!
     */
    @GetMapping("/debug/all-emails")
    public ResponseEntity<Map<String, Object>> getAllEmails() {
        List<Doctor> allDoctors = doctorRepository.findAll();
        
        Map<String, Object> debug = new HashMap<>();
        debug.put("totalDoctors", allDoctors.size());
        debug.put("emails", allDoctors.stream()
            .map(d -> Map.of(
                "email", d.getEmail(),
                "userId", d.getUserId(),
                "isActivated", d.getIsActivated(),
                "emailLength", d.getEmail().length(),
                "emailBytes", d.getEmail().getBytes().length
            ))
            .collect(Collectors.toList()));
        
        return ResponseEntity.ok(debug);
    }
    
    /**
     * Get authenticated doctor's profile
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorResponse> getDoctorProfile() {
        // Get authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        log.info("🔍 [PROFILE] Looking up doctor profile for email: '{}'", email);
        log.info("🔍 [PROFILE] Email length: {}, bytes: {}", email.length(), email.getBytes().length);
        log.info("🔍 [PROFILE] Authentication: principal={}, authorities={}", 
                authentication.getPrincipal(), authentication.getAuthorities());
        
        // FIRST: Try exact match
        log.info("🔍 [PROFILE] Attempting exact email match...");
        Optional<Doctor> doctorOpt = doctorRepository.findByEmail(email);
        
        if (doctorOpt.isEmpty()) {
            log.error("❌ [PROFILE] Exact match failed for email: '{}'", email);
            
            // DEBUG: Check what's in the database
            List<Doctor> allDoctors = doctorRepository.findAll();
            log.error("📋 [DEBUG] Total doctors in DB: {}", allDoctors.size());
            
            if (!allDoctors.isEmpty()) {
                log.error("📋 [DEBUG] All doctor emails in database:");
                allDoctors.forEach(d -> {
                    String dbEmail = d.getEmail();
                    log.error("  - DB Email: '{}' (length: {}, bytes: {})", 
                        dbEmail, dbEmail.length(), dbEmail.getBytes().length);
                    log.error("    UserId: {}, IsActivated: {}", d.getUserId(), d.getIsActivated());
                    log.error("    Equals check: {}", email.equals(dbEmail));
                    log.error("    EqualsIgnoreCase: {}", email.equalsIgnoreCase(dbEmail));
                });
                
                // Try case-insensitive search as fallback
                log.warn("⚠️ [PROFILE] Attempting case-insensitive fallback search...");
                doctorOpt = allDoctors.stream()
                    .filter(d -> d.getEmail().equalsIgnoreCase(email))
                    .findFirst();
                
                if (doctorOpt.isPresent()) {
                    log.warn("⚠️ [PROFILE] Found doctor with case-insensitive match!");
                    log.warn("⚠️ [PROFILE] Token email: '{}', DB email: '{}'", 
                        email, doctorOpt.get().getEmail());
                }
            }
        }
        
        Doctor doctor = doctorOpt.orElseThrow(() -> {
            log.error("❌ [PROFILE] Doctor not found for email: '{}'", email);
            return new RuntimeException(
                "Doctor profile not found for email: " + email + 
                ". The email in your JWT token doesn't match any doctor record in the database. " +
                "This usually means: 1) Email case mismatch, 2) Doctor record not created, " +
                "or 3) Database sync issue. Check logs above for debug information."
            );
        });
        
        log.info("✅ [PROFILE] Doctor found: id={}, email='{}', activated={}", 
                doctor.getId(), doctor.getEmail(), doctor.getIsActivated());
        
        return ResponseEntity.ok(mapToDoctorResponse(doctor));
    }
    
    /**
     * Update doctor profile
     */
    @PutMapping("/profile")
    public ResponseEntity<DoctorResponse> updateDoctorProfile(
            @RequestBody UpdateDoctorProfileRequest request,
            Authentication authentication) {
        
        String email = authentication.getName();
        
        log.info("🔄 [UPDATE] Updating profile for email: '{}'", email);
        
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Doctor not found with email: " + email));
        
        // Update allowed fields
        if (request.getFirstName() != null) {
            doctor.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            doctor.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            doctor.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getSpecialization() != null) {
            doctor.setSpecialization(request.getSpecialization());
        }
        if (request.getHospitalAffiliation() != null) {
            doctor.setHospitalAffiliation(request.getHospitalAffiliation());
        }
        if (request.getYearsOfExperience() != null) {
            doctor.setYearsOfExperience(request.getYearsOfExperience());
        }
        if (request.getOfficeAddress() != null) {
            doctor.setOfficeAddress(request.getOfficeAddress());
        }
        if (request.getConsultationHours() != null) {
            doctor.setConsultationHours(request.getConsultationHours());
        }
        
        Doctor updatedDoctor = doctorRepository.save(doctor);
        
        log.info("✅ [UPDATE] Doctor profile updated: {}", doctor.getEmail());
        
        return ResponseEntity.ok(mapToDoctorResponse(updatedDoctor));
    }
    
    /**
     * Check activation status
     */
    @GetMapping("/activation-status")
    public ResponseEntity<Map<String, Object>> getActivationStatus(Authentication authentication) {
        String email = authentication.getName();
        
        log.info("📊 [STATUS] Checking activation status for: '{}'", email);
        
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Doctor not found with email: " + email));
        
        String message = doctor.getIsActivated()
                ? "Your account is activated and ready to use"
                : "Your account is pending admin approval. You will receive an email once approved.";
        
        Map<String, Object> status = Map.of(
            "isActivated", doctor.getIsActivated(),
            "activationStatus", doctor.getActivationStatus(),
            "message", message,
            "activationRequestDate", doctor.getActivationRequestDate(),
            "activationDate", doctor.getActivationDate() != null 
                ? doctor.getActivationDate() 
                : "Not activated yet"
        );
        
        return ResponseEntity.ok(status);
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
                .build();
    }
}
