package com.healthapp.doctor.controller;

import com.healthapp.doctor.dto.request.DoctorRegisterRequest;
import com.healthapp.doctor.dto.response.DoctorResponse;
import com.healthapp.doctor.service.DoctorAuthService;
import com.healthapp.doctor.service.DoctorLoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * DoctorAuthController - Public endpoints for doctor registration and login
 * 
 * These endpoints do NOT require authentication
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Slf4j
public class DoctorAuthController {
    
    private final DoctorAuthService doctorAuthService;
    private final DoctorLoginService doctorLoginService;
    
    /**
     * Register a new doctor (PUBLIC endpoint)
     * 
     * No authentication required
     * 
     * @param request Doctor registration data
     * @return DoctorResponse with registration status
     */
    @PostMapping("/register")
    public ResponseEntity<DoctorResponse> registerDoctor(@Valid @RequestBody DoctorRegisterRequest request) {
        log.info("🏥 Doctor registration request received for: {}", request.getEmail());
        
        DoctorResponse response = doctorAuthService.registerDoctor(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Login a doctor (PUBLIC endpoint)
     * 
     * No authentication required
     * Returns access token and refresh token if successful
     * Returns error if account is not yet activated
     * 
     * @param loginRequest Email and password
     * @return Access token, refresh token, and user info
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginDoctor(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");
        
        log.info("🔐 Doctor login request for: {}", email);
        
        Map<String, Object> response = doctorLoginService.loginDoctor(email, password);
        
        // If account not activated, return 403 Forbidden
        if (response.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint
     * 
     * @return Service status
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Doctor Activation Service is UP");
    }
}
