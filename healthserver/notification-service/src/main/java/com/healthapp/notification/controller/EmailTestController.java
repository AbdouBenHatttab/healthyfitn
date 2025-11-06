package com.healthapp.notification.controller;

import com.healthapp.notification.dto.request.EmailRequest;
import com.healthapp.notification.dto.request.EmailTemplateType;
import com.healthapp.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller pour tester l'envoi d'emails facilement
 */
@RestController
@RequestMapping("/api/notifications/test")
@RequiredArgsConstructor
@Slf4j
public class EmailTestController {
    
    private final EmailService emailService;
    
    /**
     * Test simple : Envoyer un email avec texte brut
     */
    @PostMapping("/simple")
    public ResponseEntity<Map<String, String>> sendSimpleEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String text) {
        
        log.info("📧 Sending simple test email to: {}", to);
        
        try {
            EmailRequest request = EmailRequest.builder()
                    .to(to)
                    .subject(subject)
                    .textContent(text)
                    .build();
            
            emailService.sendEmail(request);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Email envoyé ! Vérifiez votre boîte : " + to
            ));
        } catch (Exception e) {
            log.error("❌ Failed to send email", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Test avec template : Email de bienvenue doctor
     */
    @PostMapping("/doctor-welcome")
    public ResponseEntity<Map<String, String>> sendDoctorWelcomeEmail(
            @RequestParam String to,
            @RequestParam String doctorName) {
        
        log.info("📧 Sending doctor welcome email to: {}", to);
        
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("doctorLastName", doctorName);
            
            EmailRequest request = EmailRequest.builder()
                    .to(to)
                    .subject("Bienvenue sur HealthApp !")
                    .templateType(EmailTemplateType.DOCTOR_REGISTRATION_PENDING)
                    .templateVariables(variables)
                    .build();
            
            emailService.sendEmail(request);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Email de bienvenue envoyé à " + to
            ));
        } catch (Exception e) {
            log.error("❌ Failed to send email", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Test : Email d'activation admin
     */
    @PostMapping("/admin-notification")
    public ResponseEntity<Map<String, String>> sendAdminNotification(
            @RequestParam String adminEmail,
            @RequestParam String doctorName,
            @RequestParam String doctorEmail) {
        
        log.info("📧 Sending admin notification to: {}", adminEmail);
        
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("adminName", "Admin");
            variables.put("doctorName", doctorName);
            variables.put("doctorEmail", doctorEmail);
            variables.put("medicalLicense", "ML-TEST-123");
            variables.put("specialization", "Cardiology");
            variables.put("hospital", "Test Hospital");
            variables.put("experience", "5");
            variables.put("registrationDate", java.time.LocalDate.now().toString());
            
            EmailRequest request = EmailRequest.builder()
                    .to(adminEmail)
                    .subject("🥼 Nouveau médecin en attente - " + doctorName)
                    .templateType(EmailTemplateType.DOCTOR_REGISTRATION_ADMIN_NOTIFICATION)
                    .templateVariables(variables)
                    .build();
            
            emailService.sendEmail(request);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Email admin envoyé à " + adminEmail
            ));
        } catch (Exception e) {
            log.error("❌ Failed to send email", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Vérifier la configuration email
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> checkEmailConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("status", "Email service is running");
        config.put("note", "Use /test/simple to send a test email");
        
        return ResponseEntity.ok(config);
    }
}