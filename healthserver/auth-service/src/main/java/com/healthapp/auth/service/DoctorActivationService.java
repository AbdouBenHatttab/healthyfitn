package com.healthapp.auth.service;

import com.healthapp.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

/**
 * Service to handle doctor activation workflow
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorActivationService {
    
    //private final EmailService emailService;

    /**
     * Create an activation request when a doctor registers
     */
    public void createActivationRequest(User doctor) {
        log.info("Creating activation request for doctor: {}", doctor.getEmail());
        // Doctor is already saved with isActivated = false
        // Just log that activation is needed
        log.info("Doctor {} pending activation", doctor.getEmail());
    }

}