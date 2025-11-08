package com.healthapp.doctor.service;

import com.healthapp.doctor.client.NotificationClient;
import com.healthapp.doctor.dto.request.DoctorActivationRequestDto;
import com.healthapp.doctor.dto.request.EmailNotificationRequest;
import com.healthapp.doctor.dto.response.DoctorPendingResponse;
import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.entity.DoctorActivationRequest;
import com.healthapp.doctor.repository.DoctorActivationRequestRepository;
import com.healthapp.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DoctorActivationService - Gestion de l'activation des médecins par les admins
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DoctorActivationService {
    
    private final DoctorRepository doctorRepository;
    private final DoctorActivationRequestRepository activationRequestRepository;
    private final NotificationClient notificationClient;

    /**
     * Récupérer tous les médecins en attente d'activation
     */
    public List<DoctorPendingResponse> getPendingDoctors() {
        log.info("📋 Fetching pending doctor requests");
        
        List<Doctor> pendingDoctors = doctorRepository.findByActivationStatus("PENDING");
        
        return pendingDoctors.stream()
                .map(this::mapToPendingResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Traiter une demande d'activation (APPROVE ou REJECT)
     */
    public void processDoctorActivation(DoctorActivationRequestDto request, String adminId, String adminEmail) {
        log.info("⚙️ Processing doctor activation request for doctor ID: {}", request.getDoctorId());
        
        // Récupérer le médecin
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + request.getDoctorId()));
        
        // Récupérer la demande d'activation
        DoctorActivationRequest activationRequest = activationRequestRepository.findByDoctorId(doctor.getId())
                .orElseThrow(() -> new RuntimeException("Activation request not found"));
        
        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            approveDoctor(doctor, activationRequest, adminId, adminEmail, request.getNotes());
        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            rejectDoctor(doctor, activationRequest, adminId, adminEmail, request.getNotes());
        } else {
            throw new IllegalArgumentException("Invalid action: " + request.getAction());
        }
    }
    
    /**
     * Approuver un médecin
     */
    private void approveDoctor(Doctor doctor, DoctorActivationRequest activationRequest, 
                               String adminId, String adminEmail, String notes) {
        log.info("✅ Approving doctor: {}", doctor.getEmail());
        
        // Mettre à jour le médecin
        doctor.setIsActivated(true);
        doctor.setActivationStatus("APPROVED");
        doctor.setActivatedBy(adminId);
        doctor.setActivationDate(LocalDateTime.now());
        doctorRepository.save(doctor);
        
        // Marquer la demande comme traitée
        activationRequest.markAsProcessed(adminId, adminEmail, "APPROVE", notes);
        activationRequestRepository.save(activationRequest);
        
        // ✅ Envoyer email de confirmation au CONTACT EMAIL
        sendActivationConfirmationEmail(doctor);
        
        log.info("✅ Doctor approved successfully: {}", doctor.getEmail());
    }
    
    /**
     * Rejeter un médecin
     */
    private void rejectDoctor(Doctor doctor, DoctorActivationRequest activationRequest,
                             String adminId, String adminEmail, String notes) {
        log.info("❌ Rejecting doctor: {}", doctor.getEmail());
        
        // Mettre à jour le médecin
        doctor.setActivationStatus("REJECTED");
        doctor.setRejectedBy(adminId);
        doctor.setRejectionDate(LocalDateTime.now());
        doctor.setRejectionReason(notes);
        doctorRepository.save(doctor);
        
        // Marquer la demande comme traitée
        activationRequest.markAsProcessed(adminId, adminEmail, "REJECT", notes);
        activationRequestRepository.save(activationRequest);
        
        // ✅ Envoyer email de rejet au CONTACT EMAIL
        sendActivationRejectionEmail(doctor, notes);
        
        log.info("❌ Doctor rejected: {}", doctor.getEmail());
    }
    
    /**
     * ✅ Envoyer email de confirmation d'activation au CONTACT EMAIL
     */
    private void sendActivationConfirmationEmail(Doctor doctor) {
        try {
            String emailTo = doctor.getNotificationEmail();
            log.info("📧 Sending activation confirmation to: {}", emailTo);
            
            EmailNotificationRequest emailRequest = EmailNotificationRequest.builder()
                    .to(emailTo) // ✅ Contact email
                    .subject("Account Activated - Welcome to Health App")
                    .templateType("DOCTOR_ACTIVATION_CONFIRMATION")
                    .templateVariables(Map.of(
                        "doctorLastName", doctor.getLastName(),
                        "doctorFirstName", doctor.getFirstName(),
                        "loginEmail", doctor.getEmail() // ✅ Rappeler l'email de connexion
                    ))
                    .build();
            
            notificationClient.sendEmail(emailRequest);
            log.info("✅ Activation confirmation sent to: {}", emailTo);
            
        } catch (Exception e) {
            log.error("❌ Failed to send activation confirmation email", e);
        }
    }
    
    /**
     * ✅ Envoyer email de rejet au CONTACT EMAIL
     */
    private void sendActivationRejectionEmail(Doctor doctor, String reason) {
        try {
            String emailTo = doctor.getNotificationEmail();
            log.info("📧 Sending rejection notification to: {}", emailTo);
            
            EmailNotificationRequest emailRequest = EmailNotificationRequest.builder()
                    .to(emailTo) // ✅ Contact email
                    .subject("Account Registration Review - Health App")
                    .templateType("DOCTOR_ACTIVATION_REJECTION")
                    .templateVariables(Map.of(
                        "doctorLastName", doctor.getLastName(),
                        "reason", reason != null ? reason : "Credentials could not be verified"
                    ))
                    .build();
            
            notificationClient.sendEmail(emailRequest);
            log.info("✅ Rejection notification sent to: {}", emailTo);
            
        } catch (Exception e) {
            log.error("❌ Failed to send rejection email", e);
        }
    }
    
    /**
     * Compter les médecins en attente
     */
    public long getPendingDoctorsCount() {
        return activationRequestRepository.countByIsPendingTrue();
    }
    
    /**
     * Mapper Doctor vers DoctorPendingResponse
     */
    private DoctorPendingResponse mapToPendingResponse(Doctor doctor) {
        DoctorActivationRequest activationRequest = activationRequestRepository
                .findByDoctorId(doctor.getId())
                .orElse(null);
        
        return DoctorPendingResponse.builder()
                .id(activationRequest != null ? activationRequest.getId() : null)
                .doctorId(doctor.getId())
                .email(doctor.getEmail())
                .fullName(doctor.getFullName())
                .medicalLicenseNumber(doctor.getMedicalLicenseNumber())
                .specialization(doctor.getSpecialization())
                .hospitalAffiliation(doctor.getHospitalAffiliation())
                .yearsOfExperience(doctor.getYearsOfExperience())
                .registrationDate(doctor.getCreatedAt())
                .activationRequestDate(doctor.getActivationRequestDate())
                .build();
    }
}