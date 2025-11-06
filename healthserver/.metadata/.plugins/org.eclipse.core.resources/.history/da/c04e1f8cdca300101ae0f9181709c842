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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * DoctorController - Endpoints pour les médecins authentifiés
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
@Slf4j
public class DoctorController {
    
    private final DoctorRepository doctorRepository;
    
    /**
     * Récupérer le profil du médecin connecté
     */
    @GetMapping("/profile")
    public ResponseEntity<DoctorResponse> getDoctorProfile(Authentication authentication) {
        String userId = authentication.getName(); // Extraire userId du JWT
        
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
        DoctorResponse response = mapToDoctorResponse(doctor);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Mettre à jour le profil du médecin
     */
    @PutMapping("/profile")
    public ResponseEntity<DoctorResponse> updateDoctorProfile(
            @RequestBody UpdateDoctorProfileRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
        // Mettre à jour les champs autorisés
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
        
        log.info("Doctor profile updated: {}", doctor.getEmail());
        
        return ResponseEntity.ok(mapToDoctorResponse(updatedDoctor));
    }
    
    /**
     * Vérifier le statut d'activation
     */
    @GetMapping("/activation-status")
    public ResponseEntity<Map<String, Object>> getActivationStatus(Authentication authentication) {
        String userId = authentication.getName();
        
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
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
     * Mapper Doctor vers DoctorResponse
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
