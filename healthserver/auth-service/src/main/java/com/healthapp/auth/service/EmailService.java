package com.healthapp.auth.service;


import com.healthapp.auth.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    
    public void sendDoctorRegistrationNotificationToAdmin(User doctor) {
        log.info("📧 ========================================");
        log.info("📧 NEW DOCTOR REGISTRATION - ADMIN NOTIFICATION");
        log.info("📧 ========================================");
        log.info("📧 Doctor Details:");
        log.info("📧   Name: {} {}", doctor.getFirstName(), doctor.getLastName());
        log.info("📧   Email: {}", doctor.getEmail());
        log.info("📧   License: {}", doctor.getMedicalLicenseNumber());
        log.info("📧   Specialization: {}", doctor.getSpecialization());
        log.info("📧   Hospital: {}", doctor.getHospitalAffiliation());
        log.info("📧   Experience: {} years", doctor.getYearsOfExperience());
        log.info("📧 ========================================");
        log.info("📧 Action Required: Review and approve this doctor");
        log.info("📧 ========================================");
    }
    
    public void sendDoctorActivationConfirmation(User doctor) {
        log.info("✅ ========================================");
        log.info("✅ DOCTOR ACCOUNT ACTIVATED");
        log.info("✅ ========================================");
        log.info("✅ Email sent to: {}", doctor.getEmail());
        log.info("✅ Message: Your account has been activated!");
        log.info("✅ You can now login to the platform.");
        log.info("✅ ========================================");
    }
    
    public void sendDoctorRejectionNotification(User doctor, String reason) {
        log.info("❌ ========================================");
        log.info("❌ DOCTOR ACCOUNT REJECTED");
        log.info("❌ ========================================");
        log.info("❌ Email sent to: {}", doctor.getEmail());
        log.info("❌ Reason: {}", reason);
        log.info("❌ ========================================");
    }
}