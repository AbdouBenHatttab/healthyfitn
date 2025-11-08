package com.healthapp.doctor.service;

import com.healthapp.doctor.dto.request.ChangePasswordRequest;
import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorPasswordService {
    
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Change doctor password
     * NOTE: MongoDB doesn't support @Transactional like JPA does
     */
    public void changePassword(String doctorId, ChangePasswordRequest request) {
        log.info("🔐 [PASSWORD] Starting password change for doctor: {}", doctorId);
        
        // Validate request
        if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
            log.error("❌ [PASSWORD] Current password is null or empty");
            throw new IllegalArgumentException("Current password is required");
        }
        
        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            log.error("❌ [PASSWORD] New password is null or empty");
            throw new IllegalArgumentException("New password is required");
        }
        
        // Find doctor
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> {
                    log.error("❌ [PASSWORD] Doctor not found with id: {}", doctorId);
                    return new RuntimeException("Doctor not found with id: " + doctorId);
                });
        
        log.info("📋 [PASSWORD] Doctor found: email={}, id={}", doctor.getEmail(), doctor.getId());
        
        // Verify current password matches
        boolean passwordMatches = passwordEncoder.matches(
            request.getCurrentPassword(), 
            doctor.getPassword()
        );
        
        log.info("🔍 [PASSWORD] Current password verification: {}", passwordMatches);
        
        if (!passwordMatches) {
            log.error("❌ [PASSWORD] Current password is incorrect for doctor: {}", doctorId);
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Validate new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), doctor.getPassword())) {
            log.error("❌ [PASSWORD] New password is same as current password");
            throw new RuntimeException("New password must be different from current password");
        }
        
        // Encode and update password
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        doctor.setPassword(encodedNewPassword);
        
        // Save to MongoDB
        try {
            doctorRepository.save(doctor);
            log.info("✅ [PASSWORD] Password changed successfully for doctor: {} (email: {})", 
                doctorId, doctor.getEmail());
        } catch (Exception e) {
            log.error("❌ [PASSWORD] Failed to save password change: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update password: " + e.getMessage());
        }
    }
}