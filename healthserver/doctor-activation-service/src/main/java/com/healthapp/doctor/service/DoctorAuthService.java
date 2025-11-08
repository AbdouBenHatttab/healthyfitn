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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DoctorAuthService - Direct doctor registration
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DoctorAuthService {
    
    private final DoctorRepository doctorRepository;
    private final DoctorActivationRequestRepository activationRequestRepository;
    private final NotificationClient notificationClient;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Value("${notification.admin-email}")
    private String adminEmail;
    
    /**
     * Register a new doctor
     */
    public DoctorResponse registerDoctor(DoctorRegisterRequest request) {
        log.info("🥼 Starting doctor registration for: {}", request.getEmail());
        log.info("📧 Contact email: {}", request.getContactEmail());
        
        // Check if doctor already exists
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Doctor already exists with email: " + request.getEmail());
        }
        
        if (doctorRepository.existsByMedicalLicenseNumber(request.getMedicalLicenseNumber())) {
            throw new RuntimeException("Medical license number already registered");
        }
        
        try {
            // Create doctor profile with contactEmail
            log.info("Step 1: Creating doctor profile");
            Doctor doctor = createDoctorProfile(request);
            Doctor savedDoctor = doctorRepository.save(doctor);
            
            // Create activation request
            log.info("Step 2: Creating activation request");
            createActivationRequest(savedDoctor);
            
            // ✅ Send email to CONTACT EMAIL (not system email)
            log.info("Step 3: Sending pending validation email to: {}", savedDoctor.getNotificationEmail());
            sendPendingValidationEmailToDoctor(savedDoctor);
            
            // Notify admins
            log.info("Step 4: Notifying admins at: {}", adminEmail);
            notifyAdmins(savedDoctor);
            
            log.info("✅ Doctor registration completed for: {}", request.getEmail());
            
            return mapToDoctorResponse(savedDoctor);
            
        } catch (Exception e) {
            log.error("❌ Failed to register doctor: {}", request.getEmail(), e);
            throw new RuntimeException("Failed to register doctor: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create doctor profile with contactEmail
     */
    private Doctor createDoctorProfile(DoctorRegisterRequest request) {
        String userId = UUID.randomUUID().toString();
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        
        return Doctor.builder()
                .userId(userId)
                .email(request.getEmail())
                .contactEmail(request.getContactEmail()) // ✅ NOUVEAU
                .password(hashedPassword)
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
     * Create activation request
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
     * ✅ Send pending validation email to doctor's CONTACT EMAIL
     */
    private void sendPendingValidationEmailToDoctor(Doctor doctor) {
        try {
            String emailTo = doctor.getNotificationEmail();
            log.info("📧 Sending pending validation email to: {}", emailTo);
            
            EmailNotificationRequest emailRequest = EmailNotificationRequest.builder()
                    .to(emailTo) // ✅ Contact email, pas system email
                    .subject("Registration Received - Pending Validation")
                    .templateType("DOCTOR_REGISTRATION_PENDING")
                    .templateVariables(Map.of(
                        "doctorFirstName", doctor.getFirstName(),
                        "doctorLastName", doctor.getLastName(),
                        "registrationDate", doctor.getCreatedAt().toString()
                    ))
                    .build();
            
            notificationClient.sendEmail(emailRequest);
            log.info("✅ Pending validation email sent to: {}", emailTo);
            
        } catch (Exception e) {
            log.error("❌ Failed to send pending validation email", e);
        }
    }
    
    /**
     * Notify admins
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
                        "doctorContactEmail", doctor.getContactEmail(), // ✅ Inclus dans notification admin
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
     * Map Doctor to DoctorResponse
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