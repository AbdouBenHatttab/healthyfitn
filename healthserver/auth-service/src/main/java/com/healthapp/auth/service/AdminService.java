package com.healthapp.auth.service;

import com.healthapp.auth.dto.response.UserResponse;
import com.healthapp.auth.entity.User;
import com.healthapp.auth.Enums.UserRole;
import com.healthapp.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Service - Handle doctor activation/rejection
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminService {
    
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailService emailService;
    
    /**
     * Get all pending doctors (isActivated = false)
     */
    public List<UserResponse> getPendingDoctors() {
        log.info("Fetching pending doctors");
        
        List<User> pendingDoctors = userRepository.findPendingDoctors();
        
        log.info("Found {} pending doctors", pendingDoctors.size());
        
        return pendingDoctors.stream()
                .map(userService::mapToUserResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Activate a doctor account
     */
    public void activateDoctor(String doctorId) {
        log.info("Activating doctor with ID: {}", doctorId);
        
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));
        
        if (!doctor.hasRole(UserRole.DOCTOR)) {
            throw new RuntimeException("User is not a doctor");
        }
        
        if (doctor.getIsActivated()) {
            log.warn("Doctor is already activated: {}", doctor.getEmail());
            return;
        }
        
        // Activate the doctor
        doctor.setIsActivated(true);
        doctor.setActivationDate(LocalDateTime.now());
        userRepository.save(doctor);
        
        // Send confirmation email
        emailService.sendDoctorActivationConfirmation(doctor);
        
        log.info("✅ Doctor activated successfully: {}", doctor.getEmail());
    }
    
    /**
     * Reject a doctor account
     */
    public void rejectDoctor(String doctorId, String reason) {
        log.info("Rejecting doctor with ID: {}", doctorId);
        
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));
        
        if (!doctor.hasRole(UserRole.DOCTOR)) {
            throw new RuntimeException("User is not a doctor");
        }
        
        // Keep isActivated = false, but mark as rejected
        // You could add a rejectionReason field to User entity if needed
        
        // Send rejection email
        emailService.sendDoctorRejectionNotification(doctor, reason);
        
        log.info("❌ Doctor rejected: {} - Reason: {}", doctor.getEmail(), reason);
    }
    
    /**
     * Get count of pending doctors
     */
    public long getPendingDoctorsCount() {
        return userRepository.findPendingDoctors().size();
    }
    
    /**
     * Get all activated doctors
     */
    public List<UserResponse> getActivatedDoctors() {
        log.info("Fetching activated doctors");
        
        List<User> activatedDoctors = userRepository.findActivatedDoctors();
        
        log.info("Found {} activated doctors", activatedDoctors.size());
        
        return activatedDoctors.stream()
                .map(userService::mapToUserResponse)
                .collect(Collectors.toList());
    }
}