package com.healthapp.doctor.service;

import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.repository.DoctorRepository;
import com.healthapp.shared.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

/**
 * DoctorLoginService - Doctor authentication management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorLoginService {
    
    private final DoctorRepository doctorRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration:86400000}")  // 24h default
    private Long jwtExpiration;
    
    /**
     * Doctor login
     */
    public Map<String, Object> loginDoctor(String email, String password) {
        log.info("🔐 Doctor login attempt for: {}", email);
        
        // Find doctor by email
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        // Verify password
        if (!passwordEncoder.matches(password, doctor.getPassword())) {
            log.error("❌ Invalid password for: {}", email);
            throw new RuntimeException("Invalid email or password");
        }
        
        // Check if account is activated
        if (!doctor.getIsActivated()) {
            log.warn("⚠️ Doctor account not activated: {}", email);
            return Map.of(
                "error", "ACCOUNT_NOT_ACTIVATED",
                "message", "Your account is pending admin approval. Please wait for activation.",
                "activationStatus", doctor.getActivationStatus(),
                "email", doctor.getEmail()
            );
        }
        
        // Generate JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", doctor.getEmail());
        claims.put("userId", doctor.getUserId());
        claims.put("roles", List.of("DOCTOR"));  // ✅ CHANGÉ: "role" → "roles" + List.of()
        claims.put("doctorId", doctor.getId());
        
        // Call static method directly (no injection needed)
        String accessToken = JwtUtil.generateToken(claims, doctor.getEmail(), jwtExpiration, jwtSecret);
        String refreshToken = JwtUtil.generateToken(claims, doctor.getEmail(), jwtExpiration * 7, jwtSecret);
        
        log.info("✅ Login successful for doctor: {}", email);
        
        return Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken,
            "userId", doctor.getUserId(),
            "doctorId", doctor.getId(),
            "email", doctor.getEmail(),
            "fullName", doctor.getFullName(),
            "isActivated", doctor.getIsActivated(),
            "role", "DOCTOR"
        );
    }
}