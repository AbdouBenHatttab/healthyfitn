package com.healthapp.doctor.controller;

import com.healthapp.doctor.dto.request.AppointmentResponseRequest;
import com.healthapp.doctor.dto.response.AppointmentResponse;
import com.healthapp.doctor.dto.response.DoctorStatsResponse;
import com.healthapp.doctor.dto.response.PatientInfoResponse;
import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.repository.DoctorRepository;
import com.healthapp.doctor.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur des rendez-vous pour les médecins avec Keycloak
 *
 * ✅ Intégration Keycloak OAuth2
 * - Authentification via JWT Keycloak
 * - Extraction de l'email depuis le token
 * - Vérification du rôle DOCTOR
 */
@RestController
@RequestMapping("/api/doctors/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
@Slf4j
public class DoctorAppointmentController {

    private final AppointmentService appointmentService;
    private final DoctorRepository doctorRepository;

    /**
     * 🔐 Extraire l'email du token JWT Keycloak
     *
     * Le token Keycloak contient plusieurs claims possibles pour l'email:
     * - "email" (claim standard)
     * - "preferred_username" (souvent utilisé comme email)
     */
    private String extractEmailFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            log.error("❌ Authentication is null or principal is null");
            throw new RuntimeException("Authentication required");
        }

        log.debug("🔍 Authentication type: {}", auth.getClass().getName());
        log.debug("🔍 Principal type: {}", auth.getPrincipal().getClass().getName());

        // Cas 1: JWT Token (Keycloak)
        if (auth.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) auth.getPrincipal();

            log.debug("🔍 JWT Claims: {}", jwt.getClaims().keySet());

            // Essayer d'abord le claim "email"
            String email = jwt.getClaim("email");
            if (email != null && !email.isEmpty()) {
                log.info("✅ Email extracted from JWT 'email' claim: {}", email);
                return email;
            }

            // Sinon essayer "preferred_username"
            email = jwt.getClaim("preferred_username");
            if (email != null && !email.isEmpty()) {
                log.info("✅ Email extracted from JWT 'preferred_username' claim: {}", email);
                return email;
            }

            // Dernier recours: le subject
            email = jwt.getSubject();
            if (email != null && !email.isEmpty()) {
                log.info("⚠️ Email extracted from JWT 'sub' claim: {}", email);
                return email;
            }

            log.error("❌ No email found in JWT token");
            throw new RuntimeException("Email not found in authentication token");
        }

        // Cas 2: Authentication simple (fallback)
        String name = auth.getName();
        if (name != null && !name.isEmpty()) {
            log.info("✅ Email extracted from Authentication.getName(): {}", name);
            return name;
        }

        log.error("❌ Could not extract email from authentication");
        throw new RuntimeException("Unable to extract user email from authentication");
    }

    /**
     * 🔐 Récupérer le docteur connecté à partir du token
     */
    private Doctor getAuthenticatedDoctor(Authentication auth) {
        String email = extractEmailFromAuth(auth);

        log.debug("🔍 Looking for doctor with email: {}", email);

        Doctor doctor = doctorRepository.findByContactEmail(email)
                .orElseThrow(() -> {
                    log.error("❌ Doctor not found in database for email: {}", email);
                    return new RuntimeException("Doctor profile not found. Please contact support.");
                });

        // Vérifier que le compte est activé
        if (!doctor.getIsActivated()) {
            log.warn("⚠️ Doctor account not activated: {}", email);
            throw new RuntimeException("Your account is not activated yet. Please wait for admin approval.");
        }

        log.info("✅ Authenticated doctor: {} (ID: {})", doctor.getFullName(), doctor.getId());
        return doctor;
    }

    /**
     * Obtenir tous les rendez-vous du médecin connecté
     */
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(Authentication auth) {
        log.info("========================================");
        log.info("📅 GET ALL APPOINTMENTS REQUEST");
        log.info("========================================");

        Doctor doctor = getAuthenticatedDoctor(auth);
        log.info("📋 Fetching all appointments for doctor: {}", doctor.getFullName());

        List<AppointmentResponse> appointments = appointmentService.getDoctorAppointments(doctor.getId());

        log.info("✅ Found {} appointments", appointments.size());
        return ResponseEntity.ok(appointments);
    }

    /**
     * Obtenir uniquement les rendez-vous à venir
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<AppointmentResponse>> getUpcomingAppointments(Authentication auth) {
        log.info("========================================");
        log.info("📅 GET UPCOMING APPOINTMENTS REQUEST");
        log.info("========================================");

        Doctor doctor = getAuthenticatedDoctor(auth);
        log.info("📋 Fetching upcoming appointments for doctor: {}", doctor.getFullName());

        List<AppointmentResponse> appointments = appointmentService.getUpcomingAppointments(doctor.getId());

        log.info("✅ Found {} upcoming appointments", appointments.size());
        return ResponseEntity.ok(appointments);
    }

    /**
     * Obtenir les rendez-vous en attente (à répondre)
     */
    @GetMapping("/pending")
    public ResponseEntity<List<AppointmentResponse>> getPendingAppointments(Authentication auth) {
        log.info("========================================");
        log.info("📋 GET PENDING APPOINTMENTS REQUEST");
        log.info("========================================");

        Doctor doctor = getAuthenticatedDoctor(auth);
        log.info("📋 Fetching pending appointments for doctor: {}", doctor.getFullName());

        List<AppointmentResponse> appointments = appointmentService.getPendingAppointments(doctor.getId());

        log.info("✅ Found {} pending appointments", appointments.size());
        return ResponseEntity.ok(appointments);
    }

    /**
     * Accepter un rendez-vous en attente
     */
    @PostMapping("/{appointmentId}/accept")
    public ResponseEntity<AppointmentResponse> acceptAppointment(
            @PathVariable String appointmentId,
            Authentication auth) {

        log.info("========================================");
        log.info("✅ ACCEPT APPOINTMENT REQUEST");
        log.info("========================================");

        Doctor doctor = getAuthenticatedDoctor(auth);
        log.info("✅ Doctor {} accepting appointment: {}", doctor.getFullName(), appointmentId);

        AppointmentResponse response = appointmentService.acceptAppointment(
                appointmentId, doctor.getId());

        log.info("✅ Appointment accepted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Rejeter un rendez-vous en attente avec une raison
     */
    @PostMapping("/{appointmentId}/reject")
    public ResponseEntity<AppointmentResponse> rejectAppointment(
            @PathVariable String appointmentId,
            @RequestBody AppointmentResponseRequest request,
            Authentication auth) {

        log.info("========================================");
        log.info("❌ REJECT APPOINTMENT REQUEST");
        log.info("========================================");

        Doctor doctor = getAuthenticatedDoctor(auth);
        log.info("❌ Doctor {} rejecting appointment: {}", doctor.getFullName(), appointmentId);

        // Validation
        if (request.getReason() == null || request.getReason().isBlank()) {
            log.error("❌ Rejection reason is required");
            throw new RuntimeException("Une raison est requise pour le rejet");
        }

        AppointmentResponse response = appointmentService.rejectAppointment(
                appointmentId,
                doctor.getId(),
                request.getReason(),
                request.getAvailableHours());

        log.info("✅ Appointment rejected successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Obtenir la liste des patients du médecin connecté
     */
    @GetMapping("/patients")
    public ResponseEntity<List<PatientInfoResponse>> getMyPatients(Authentication auth) {
        log.info("========================================");
        log.info("👥 GET PATIENTS LIST REQUEST");
        log.info("========================================");

        Doctor doctor = getAuthenticatedDoctor(auth);
        log.info("👥 Fetching patients list for doctor: {}", doctor.getFullName());

        List<PatientInfoResponse> patients = appointmentService.getDoctorPatients(doctor.getId());

        log.info("✅ Found {} patients", patients.size());
        return ResponseEntity.ok(patients);
    }

    /**
     * Obtenir les statistiques du tableau de bord
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DoctorStatsResponse> getDashboardStats(Authentication auth) {
        log.info("========================================");
        log.info("📊 GET DASHBOARD STATS REQUEST");
        log.info("========================================");

        Doctor doctor = getAuthenticatedDoctor(auth);
        log.info("📊 Generating dashboard stats for doctor: {}", doctor.getFullName());

        DoctorStatsResponse stats = appointmentService.getDoctorStats(doctor.getId());

        log.info("✅ Dashboard stats generated successfully");
        return ResponseEntity.ok(stats);
    }

    /**
     * Terminer un rendez-vous
     */
    @PostMapping("/{appointmentId}/complete")
    public ResponseEntity<AppointmentResponse> completeAppointment(
            @PathVariable String appointmentId,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        log.info("========================================");
        log.info("✅ COMPLETE APPOINTMENT REQUEST");
        log.info("========================================");

        Doctor doctor = getAuthenticatedDoctor(auth);
        log.info("✅ Doctor {} completing appointment: {}", doctor.getFullName(), appointmentId);

        String diagnosis = body.get("diagnosis");
        String prescription = body.get("prescription");
        String notes = body.get("notes");

        // Vérifier que c'est bien le médecin du rendez-vous
        AppointmentResponse response = appointmentService.completeAppointment(
                appointmentId, doctor.getId(), diagnosis, prescription, notes);

        log.info("✅ Appointment completed successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Annuler un rendez-vous (côté médecin)
     */
    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<Map<String, String>> cancelAppointment(
            @PathVariable String appointmentId,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        log.info("========================================");
        log.info("❌ CANCEL APPOINTMENT REQUEST");
        log.info("========================================");

        Doctor doctor = getAuthenticatedDoctor(auth);
        log.info("❌ Doctor {} cancelling appointment: {}", doctor.getFullName(), appointmentId);

        String reason = (body != null) ? body.get("reason") : "Aucune raison fournie";

        // Vérifier que c'est bien le médecin du rendez-vous
        appointmentService.cancelAppointment(appointmentId, doctor.getId(), "DOCTOR", reason);

        log.info("✅ Appointment cancelled successfully");
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Rendez-vous annulé avec succès"
        ));
    }
}