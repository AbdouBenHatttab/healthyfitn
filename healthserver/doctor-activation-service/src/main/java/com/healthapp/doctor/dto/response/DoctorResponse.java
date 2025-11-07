package com.healthapp.doctor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorResponse {
    
    private String id;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    
    // Informations médicales
    private String medicalLicenseNumber;
    private String specialization;
    private String hospitalAffiliation;
    private Integer yearsOfExperience;
    private String officeAddress;
    private String consultationHours;
    
    // Statut
    private Boolean isActivated;
    private String activationStatus;
    private LocalDateTime activationDate;
    private LocalDateTime activationRequestDate;
    
    // Statistiques
    private Integer totalPatients;
    private Double averageRating;
    private Integer totalConsultations;
    
    private LocalDateTime createdAt;
    private String profilePictureUrl;
}
