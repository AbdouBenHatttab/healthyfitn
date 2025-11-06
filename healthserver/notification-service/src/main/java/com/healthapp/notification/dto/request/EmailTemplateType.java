package com.healthapp.notification.dto.request;

/**
 * Types de templates d'email disponibles
 */
public enum EmailTemplateType {
    // Emails pour les médecins
    DOCTOR_REGISTRATION_PENDING,              // NOUVEAU : Email au médecin après inscription
    DOCTOR_REGISTRATION_ADMIN_NOTIFICATION,   // Email aux admins
    DOCTOR_ACTIVATION_CONFIRMATION,            // Email au médecin quand approuvé
    DOCTOR_ACTIVATION_REJECTION,               // Email au médecin si rejeté
    
    // Emails généraux
    USER_WELCOME,
    PASSWORD_RESET,
    EMAIL_VERIFICATION,
    ACCOUNT_LOCKED,
    
    // Emails pour les patients
    APPOINTMENT_CONFIRMATION,
    APPOINTMENT_REMINDER,
    PRESCRIPTION_READY,
    
    // Autres
    SECURITY_ALERT,
    NEWSLETTER
}