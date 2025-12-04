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
 * DoctorActivationService - Gestion de l'activation des médecins avec Keycloak
 * ✅ Active le compte dans MongoDB + Keycloak
 * ✅ Envoie un email pour définir le mot de passe
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DoctorActivationService {

    private final DoctorRepository doctorRepository;
    private final DoctorActivationRequestRepository activationRequestRepository;
    private final NotificationClient notificationClient;
    private final KeycloakUserService keycloakUserService;

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
     * Récupérer tous les médecins activés
     */
    public List<DoctorPendingResponse> getActivatedDoctors() {
        log.info("📋 Fetching activated doctor requests");

        List<Doctor> activatedDoctors = doctorRepository.findByActivationStatus("APPROVED");

        log.info("✅ Found {} activated doctors", activatedDoctors.size());

        return activatedDoctors.stream()
                .map(this::mapToPendingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Approuver un médecin
     * ✅ Active dans MongoDB + Keycloak
     * ✅ Envoie un email pour définir le mot de passe
     */
    private void approveDoctor(Doctor doctor, DoctorActivationRequest activationRequest,
                               String adminId, String adminEmail, String notes) {
        log.info("========================================");
        log.info("✅ APPROVING DOCTOR: {}", doctor.getEmail());
        log.info("========================================");

        try {
            // ✅ STEP 1: Activer dans MongoDB
            log.info("📝 STEP 1: Activating in MongoDB");
            doctor.setIsActivated(true);
            doctor.setActivationStatus("APPROVED");
            doctor.setActivatedBy(adminId);
            doctor.setActivationDate(LocalDateTime.now());
            doctorRepository.save(doctor);

            log.info("✅ Doctor activated in MongoDB");

            // ✅ STEP 2: Activer dans Keycloak
            log.info("========================================");
            log.info("🔐 STEP 2: Activating in Keycloak");
            log.info("========================================");
            log.info("Keycloak User ID: {}", doctor.getUserId());

            keycloakUserService.enableDoctorUser(doctor.getUserId());

            log.info("✅ Doctor activated in Keycloak");
            log.info("📧 Password setup email sent by Keycloak");

            // ✅ STEP 3: Marquer la demande comme traitée
            log.info("📋 STEP 3: Marking activation request as processed");
            activationRequest.markAsProcessed(adminId, adminEmail, "APPROVE", notes);
            activationRequestRepository.save(activationRequest);

            // ✅ STEP 4: Envoyer email de confirmation au médecin
            log.info("========================================");
            log.info("📧 STEP 4: Sending activation confirmation email");
            log.info("========================================");
            sendActivationConfirmationEmail(doctor);

            log.info("========================================");
            log.info("✅ DOCTOR APPROVAL COMPLETED");
            log.info("========================================");
            log.info("Doctor: {}", doctor.getEmail());
            log.info("Status: ACTIVATED");
            log.info("Next step: Doctor will receive password setup email from Keycloak");
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ Failed to approve doctor", e);
            throw new RuntimeException("Failed to approve doctor: " + e.getMessage(), e);
        }
    }

    /**
     * Rejeter un médecin
     * ✅ Marque comme rejeté dans MongoDB
     * ✅ Désactive dans Keycloak
     */
    private void rejectDoctor(Doctor doctor, DoctorActivationRequest activationRequest,
                              String adminId, String adminEmail, String notes) {
        log.info("========================================");
        log.info("❌ REJECTING DOCTOR: {}", doctor.getEmail());
        log.info("========================================");

        try {
            // ✅ STEP 1: Mettre à jour MongoDB
            log.info("📝 STEP 1: Updating rejection in MongoDB");
            doctor.setActivationStatus("REJECTED");
            doctor.setRejectedBy(adminId);
            doctor.setRejectionDate(LocalDateTime.now());
            doctor.setRejectionReason(notes);
            doctorRepository.save(doctor);

            // ✅ STEP 2: Désactiver dans Keycloak
            log.info("🔐 STEP 2: Disabling in Keycloak");
            keycloakUserService.disableDoctorUser(doctor.getUserId(), notes);

            log.info("✅ Doctor disabled in Keycloak");

            // ✅ STEP 3: Marquer la demande comme traitée
            log.info("📋 STEP 3: Marking activation request as processed");
            activationRequest.markAsProcessed(adminId, adminEmail, "REJECT", notes);
            activationRequestRepository.save(activationRequest);

            // ✅ STEP 4: Envoyer email de rejet
            log.info("📧 STEP 4: Sending rejection notification");
            sendActivationRejectionEmail(doctor, notes);

            log.info("========================================");
            log.info("✅ DOCTOR REJECTION COMPLETED");
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ Failed to reject doctor", e);
            throw new RuntimeException("Failed to reject doctor: " + e.getMessage(), e);
        }
    }

    /**
     * Envoyer email de confirmation d'activation
     */
    private void sendActivationConfirmationEmail(Doctor doctor) {
        try {
            String emailTo = doctor.getNotificationEmail();
            log.info("📧 Sending activation confirmation to: {}", emailTo);

            EmailNotificationRequest emailRequest = EmailNotificationRequest.builder()
                    .to(emailTo)
                    .subject("Account Activated - Set Your Password")
                    .templateType("DOCTOR_ACTIVATION_CONFIRMATION")
                    .templateVariables(Map.of(
                            "doctorLastName", doctor.getLastName(),
                            "doctorFirstName", doctor.getFirstName(),
                            "loginEmail", doctor.getEmail(),
                            "note", "You will receive a separate email from Keycloak to set your password."
                    ))
                    .build();

            notificationClient.sendEmail(emailRequest);
            log.info("✅ Activation confirmation sent to: {}", emailTo);

        } catch (Exception e) {
            log.error("❌ Failed to send activation confirmation email", e);
        }
    }

    /**
     * Envoyer email de rejet
     */
    private void sendActivationRejectionEmail(Doctor doctor, String reason) {
        try {
            String emailTo = doctor.getNotificationEmail();
            log.info("📧 Sending rejection notification to: {}", emailTo);

            EmailNotificationRequest emailRequest = EmailNotificationRequest.builder()
                    .to(emailTo)
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