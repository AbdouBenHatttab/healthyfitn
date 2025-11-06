package com.healthapp.notification.service;

import com.healthapp.notification.dto.request.EmailRequest;
import com.healthapp.notification.dto.request.EmailTemplateType;
import com.healthapp.notification.entity.NotificationLog;
import com.healthapp.notification.entity.NotificationStatus;
import com.healthapp.notification.entity.NotificationType;
import com.healthapp.notification.repository.NotificationLogRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * EmailService - Service d'envoi d'emails
 * 
 * Ce service gère:
 * - L'envoi d'emails avec templates Thymeleaf
 * - L'envoi asynchrone pour ne pas bloquer les autres services
 * - Le logging de tous les envois
 * - La gestion des erreurs avec circuit breaker
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final NotificationLogRepository notificationLogRepository;
    
    @Value("${spring.mail.username:noreply@healthapp.com}")
    private String fromEmail;
    
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;
    
    /**
     * Envoi asynchrone d'email avec gestion d'erreur
     * 
     * @CircuitBreaker: Si le service d'email tombe, on bascule vers emailFallback
     * @Async: L'envoi se fait dans un thread séparé
     */
    @Async
    @CircuitBreaker(name = "emailService", fallbackMethod = "emailFallback")
    public void sendEmail(EmailRequest request) {
        log.info("📧 Sending email to: {}", request.getTo());
        
        // Créer un log de notification
        NotificationLog notificationLog = createNotificationLog(request);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            
            // Gestion des CC et BCC
            if (request.getCc() != null && request.getCc().length > 0) {
                helper.setCc(request.getCc());
            }
            if (request.getBcc() != null && request.getBcc().length > 0) {
                helper.setBcc(request.getBcc());
            }
            
            // Génération du contenu
            String content;
            if (request.getTemplateType() != null) {
                // Utiliser un template Thymeleaf
                content = processTemplate(request.getTemplateType(), request.getTemplateVariables());
                helper.setText(content, true);  // true = HTML
            } else if (request.getHtmlContent() != null) {
                helper.setText(request.getHtmlContent(), true);
            } else if (request.getTextContent() != null) {
                helper.setText(request.getTextContent(), false);
            } else {
                throw new IllegalArgumentException("No content provided for email");
            }
            
            // Envoyer l'email
            mailSender.send(message);
            
            // Marquer comme envoyé
            notificationLog.setStatus(NotificationStatus.SENT);
            notificationLog.setSentAt(LocalDateTime.now());
            notificationLogRepository.save(notificationLog);
            
            log.info("✅ Email sent successfully to: {}", request.getTo());
            
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to: {}", request.getTo(), e);
            notificationLog.setStatus(NotificationStatus.FAILED);
            notificationLog.setErrorMessage(e.getMessage());
            notificationLogRepository.save(notificationLog);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    /**
     * Méthode de fallback en cas d'échec
     */
    // private void emailFallback(EmailRequest request, Exception e) {
    //     log.error("🔴 Circuit breaker activated - Email service is down!");
    //     log.error("Failed to send email to: {} - Error: {}", request.getTo(), e.getMessage());
        
    //     // Sauvegarder pour retry plus tard
    //     NotificationLog failedLog = NotificationLog.builder()
    //             .type(NotificationType.EMAIL)
    //             .recipient(request.getTo())
    //             .subject(request.getSubject())
    //             .status(NotificationStatus.RETRY)
    //             .errorMessage("Circuit breaker activated: " + e.getMessage())
    //             .retryCount(0)
    //             .createdAt(LocalDateTime.now())
    //             .build();
        
    //     notificationLogRepository.save(failedLog);
    // }
    
    /**
     * Traiter un template Thymeleaf
     */
    private String processTemplate(EmailTemplateType templateType, Map<String, Object> variables) {
        Context context = new Context();
        
        // Ajouter les variables du template
        if (variables != null) {
            variables.forEach(context::setVariable);
        }
        
        // Ajouter des variables globales
        context.setVariable("frontendUrl", frontendUrl);
        context.setVariable("year", LocalDateTime.now().getYear());
        
        // Sélectionner le template approprié
        String templateName = getTemplateName(templateType);
        
        return templateEngine.process(templateName, context);
    }
    
    private String getTemplateName(EmailTemplateType templateType) {
    return switch (templateType) {
        case DOCTOR_REGISTRATION_PENDING -> "doctor-registration-pending";  // ✅ AJOUTER
        case DOCTOR_REGISTRATION_ADMIN_NOTIFICATION -> "doctor-registration-admin";
        case DOCTOR_ACTIVATION_CONFIRMATION -> "doctor-activation-confirmation";
        case DOCTOR_ACTIVATION_REJECTION -> "doctor-activation-rejection";
        case USER_WELCOME -> "user-welcome";
        case PASSWORD_RESET -> "password-reset";
        case EMAIL_VERIFICATION -> "email-verification";
        case ACCOUNT_LOCKED -> "account-locked";
        default -> throw new IllegalArgumentException("Unknown template type: " + templateType);
    };
}
    /**
     * Créer un log de notification
     */
    private NotificationLog createNotificationLog(EmailRequest request) {
        return NotificationLog.builder()
                .type(NotificationType.EMAIL)
                .recipient(request.getTo())
                .subject(request.getSubject())
                .templateType(request.getTemplateType() != null ? request.getTemplateType().name() : null)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }
}