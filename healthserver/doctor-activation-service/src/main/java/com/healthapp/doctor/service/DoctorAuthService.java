package com.healthapp.doctor.service;

import com.healthapp.doctor.client.NotificationClient;
import com.healthapp.doctor.dto.request.DoctorRegisterRequest;
import com.healthapp.doctor.dto.request.EmailNotificationRequest;
import com.healthapp.doctor.dto.response.DoctorResponse;
import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.entity.DoctorActivationRequest;
import com.healthapp.doctor.repository.DoctorActivationRequestRepository;
import com.healthapp.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DoctorAuthService - Enregistrement des médecins avec Keycloak
 * ⚠️ SANS MOT DE PASSE - Le mot de passe sera défini après activation par l'admin
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DoctorAuthService {

    private final DoctorRepository doctorRepository;
    private final DoctorActivationRequestRepository activationRequestRepository;
    private final NotificationClient notificationClient;
    private final KeycloakUserService keycloakUserService;

    @Value("${notification.admin-email}")
    private String adminEmail;

    /**
     * Enregistrer un nouveau médecin
     * ✅ Créé dans MongoDB + Keycloak (désactivé, sans mot de passe)
     */
    public DoctorResponse registerDoctor(DoctorRegisterRequest request) {
        log.info("========================================");
        log.info("🥼 DOCTOR REGISTRATION START (KEYCLOAK)");
        log.info("========================================");
        log.info("📧 System Email (login): {}", request.getEmail());
        log.info("📨 Contact Email (notifications): {}", request.getContactEmail());
        log.info("========================================");

        // Validation initiale
        if (request.getContactEmail() == null || request.getContactEmail().trim().isEmpty()) {
            log.error("❌ CRITICAL: contactEmail is NULL or EMPTY in request!");
            throw new RuntimeException("Contact email is required for doctor registration");
        }

        // Vérifier si le médecin existe déjà dans MongoDB
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Doctor already exists with email: " + request.getEmail());
        }

        if (doctorRepository.existsByMedicalLicenseNumber(request.getMedicalLicenseNumber())) {
            throw new RuntimeException("Medical license number already registered");
        }

        // Vérifier si l'utilisateur existe déjà dans Keycloak
        if (keycloakUserService.userExists(request.getEmail())) {
            throw new RuntimeException("User already exists in Keycloak with email: " + request.getEmail());
        }

        try {
            // ✅ STEP 1: Créer le profil doctor dans MongoDB (SANS MOT DE PASSE)
            log.info("📝 STEP 1: Creating doctor profile in MongoDB (no password)");
            Doctor doctor = createDoctorProfileWithoutPassword(request);

            log.info("🔍 Doctor object BEFORE save:");
            log.info("   - email: {}", doctor.getEmail());
            log.info("   - contactEmail: {}", doctor.getEmail());
            log.info("   - userId: {}", doctor.getUserId());
            log.info("   - password: NULL");

            // ✅ STEP 2: Sauvegarder dans MongoDB
            log.info("💾 STEP 2: Saving to MongoDB");
            Doctor savedDoctor = doctorRepository.save(doctor);

            log.info("✅ Doctor saved to MongoDB:");
            log.info("   - ID: {}", savedDoctor.getId());
            log.info("   - userId: {}", savedDoctor.getUserId());
            log.info("   - email: {}", savedDoctor.getEmail());
            log.info("   - contactEmail: {}", savedDoctor.getContactEmail());

            // ✅ STEP 3: Créer l'utilisateur dans Keycloak (SANS MOT DE PASSE, DÉSACTIVÉ)
            log.info("========================================");
            log.info("🔐 STEP 3: Creating user in Keycloak");
            log.info("========================================");

            String keycloakUserId = keycloakUserService.createDoctorUserWithoutPassword(
                    savedDoctor.getEmail(),
                    savedDoctor.getFirstName(),
                    savedDoctor.getLastName(),
                    savedDoctor.getUserId()
            );

            log.info("✅ User created in Keycloak with ID: {}", keycloakUserId);

            // Stocker le Keycloak ID dans MongoDB pour référence
            savedDoctor.setUserId(keycloakUserId);
            doctorRepository.save(savedDoctor);

            // ✅ STEP 4: Créer la demande d'activation
            log.info("📋 STEP 4: Creating activation request");
            createActivationRequest(savedDoctor);

            // ✅ STEP 5: Envoyer email au DOCTOR
            log.info("========================================");
            log.info("📧 STEP 5: Sending email to DOCTOR");
            log.info("========================================");
            log.info("🎯 Target email: {}", savedDoctor.getNotificationEmail());
            log.info("📝 Template: DOCTOR_REGISTRATION_PENDING");
            log.info("========================================");

            sendPendingValidationEmailToDoctor(savedDoctor);

            // ✅ STEP 6: Envoyer email à l'ADMIN
            log.info("========================================");
            log.info("📧 STEP 6: Sending email to ADMIN");
            log.info("========================================");
            log.info("🎯 Admin email: {}", adminEmail);
            log.info("📝 Template: DOCTOR_REGISTRATION_ADMIN_NOTIFICATION");
            log.info("========================================");

            notifyAdmins(savedDoctor);

            log.info("========================================");
            log.info("✅ DOCTOR REGISTRATION COMPLETED");
            log.info("========================================");
            log.info("⚠️ Doctor account created WITHOUT password");
            log.info("⚠️ Doctor will receive password setup email after admin activation");
            log.info("========================================");

            return mapToDoctorResponse(savedDoctor);

        } catch (Exception e) {
            log.error("❌ Failed to register doctor: {}", request.getEmail(), e);
            throw new RuntimeException("Failed to register doctor: " + e.getMessage(), e);
        }
    }

    /**
     * Créer le profil doctor SANS MOT DE PASSE
     */
    private Doctor createDoctorProfileWithoutPassword(DoctorRegisterRequest request) {
        String userId = UUID.randomUUID().toString();

        return Doctor.builder()
                .userId(userId)
                .email(request.getEmail())
                .contactEmail(request.getContactEmail())
                //.password(null) // ⚠️ PAS DE MOT DE PASSE
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .medicalLicenseNumber(request.getMedicalLicenseNumber())
                .specialization(request.getSpecialization())
                .hospitalAffiliation(request.getHospitalAffiliation())
                .yearsOfExperience(request.getYearsOfExperience())
                .officeAddress(request.getOfficeAddress())
                .consultationHours(request.getConsultationHours())
                .isActivated(false)
                .activationStatus("PENDING")
                .activationRequestDate(LocalDateTime.now())
                .totalPatients(0)
                .totalConsultations(0)
                .averageRating(0.0)
                .build();
    }

    /**
     * Créer la demande d'activation
     */
    private void createActivationRequest(Doctor doctor) {
        DoctorActivationRequest activationRequest = DoctorActivationRequest.builder()
                .doctorId(doctor.getId())
                .doctorEmail(doctor.getEmail())
                .doctorFullName(doctor.getFullName())
                .medicalLicenseNumber(doctor.getMedicalLicenseNumber())
                .specialization(doctor.getSpecialization())
                .hospitalAffiliation(doctor.getHospitalAffiliation())
                .yearsOfExperience(doctor.getYearsOfExperience())
                .isPending(true)
                .requestedAt(LocalDateTime.now())
                .build();

        activationRequestRepository.save(activationRequest);
    }

    /**
     * Envoyer email de confirmation au médecin
     */
    private void sendPendingValidationEmailToDoctor(Doctor doctor) {
        try {
            String emailTo = doctor.getNotificationEmail();
            log.info("📧 Sending pending validation email to: {}", emailTo);

            EmailNotificationRequest emailRequest = EmailNotificationRequest.builder()
                    .to(emailTo)
                    .subject("Registration Received - Pending Validation")
                    .templateType("DOCTOR_REGISTRATION_PENDING")
                    .templateVariables(Map.of(
                            "doctorFirstName", doctor.getFirstName(),
                            "doctorLastName", doctor.getLastName(),
                            "registrationDate", doctor.getCreatedAt().toString(),
                            "note", "You will receive an email to set your password once your account is activated."
                    ))
                    .build();

            notificationClient.sendEmail(emailRequest);
            log.info("✅ Pending validation email sent to: {}", emailTo);

        } catch (Exception e) {
            log.error("❌ Failed to send pending validation email", e);
        }
    }

    /**
     * Notifier les admins
     */
    private void notifyAdmins(Doctor doctor) {
        try {
            log.info("📧 Sending notification to admin: {}", adminEmail);

            EmailNotificationRequest emailRequest = EmailNotificationRequest.builder()
                    .to(adminEmail)
                    .subject("New Doctor Registration - Approval Required")
                    .templateType("DOCTOR_REGISTRATION_ADMIN_NOTIFICATION")
                    .templateVariables(Map.of(
                            "adminName", "Admin",
                            "doctorName", doctor.getFullName(),
                            "doctorEmail", doctor.getEmail(),
                            "doctorContactEmail", doctor.getContactEmail(),
                            "medicalLicense", doctor.getMedicalLicenseNumber(),
                            "specialization", doctor.getSpecialization(),
                            "hospital", doctor.getHospitalAffiliation(),
                            "experience", doctor.getYearsOfExperience(),
                            "registrationDate", doctor.getCreatedAt().toString()
                    ))
                    .build();

            notificationClient.sendEmail(emailRequest);
            log.info("✅ Admin notification sent to: {}", adminEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send admin notification", e);
        }
    }

    /**
     * Mapper vers DoctorResponse
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