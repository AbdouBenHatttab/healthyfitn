package com.healthapp.doctor.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * DoctorActivationRequest - Demande d'activation d'un compte médecin
 * 
 * Cette entité garde l'historique de toutes les demandes d'activation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "doctor_activation_requests")
public class DoctorActivationRequest {
    
    @Id
    private String id;
    
    @Indexed
    private String doctorId;
    
    private String doctorEmail;
    private String doctorFullName;
    private String medicalLicenseNumber;
    private String specialization;
    private String hospitalAffiliation;
    private Integer yearsOfExperience;
    
    @Builder.Default
    private Boolean isPending = true;
    
    private String action; // APPROVE or REJECT
    
    private String processedBy; // Admin ID
    private String processedByEmail;
    private LocalDateTime processedAt;
    private String processingNotes;
    
    @CreatedDate
    private LocalDateTime requestedAt;
    
    public void markAsProcessed(String adminId, String adminEmail, String action, String notes) {
        this.isPending = false;
        this.action = action;
        this.processedBy = adminId;
        this.processedByEmail = adminEmail;
        this.processedAt = LocalDateTime.now();
        this.processingNotes = notes;
    }
}