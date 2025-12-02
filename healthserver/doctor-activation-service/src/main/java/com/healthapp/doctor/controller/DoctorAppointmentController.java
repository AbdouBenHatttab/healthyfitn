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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur des rendez-vous pour les médecins
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
     * Obtenir tous les rendez-vous du médecin connecté
     */
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(Authentication auth) {
        String email = auth.getName();
        log.info("📅 Médecin {} demande tous ses rendez-vous", email);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin introuvable"));

        List<AppointmentResponse> appointments = appointmentService.getDoctorAppointments(doctor.getId());

        return ResponseEntity.ok(appointments);
    }

    /**
     * Obtenir uniquement les rendez-vous à venir
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<AppointmentResponse>> getUpcomingAppointments(Authentication auth) {
        String email = auth.getName();
        log.info("📅 Médecin {} demande ses rendez-vous à venir", email);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin introuvable"));

        List<AppointmentResponse> appointments = appointmentService.getUpcomingAppointments(doctor.getId());

        return ResponseEntity.ok(appointments);
    }

    /**
     * Obtenir les rendez-vous en attente (à répondre)
     */
    @GetMapping("/pending")
    public ResponseEntity<List<AppointmentResponse>> getPendingAppointments(Authentication auth) {
        String email = auth.getName();
        log.info("📋 Médecin {} demande ses rendez-vous en attente", email);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin introuvable"));

        List<AppointmentResponse> appointments = appointmentService.getPendingAppointments(doctor.getId());

        return ResponseEntity.ok(appointments);
    }

    /**
     * Accepter un rendez-vous en attente
     */
    @PostMapping("/{appointmentId}/accept")
    public ResponseEntity<AppointmentResponse> acceptAppointment(
            @PathVariable String appointmentId,
            Authentication auth) {

        String email = auth.getName();
        log.info("✅ Médecin {} accepte le rendez-vous {}", email, appointmentId);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin introuvable"));

        AppointmentResponse response = appointmentService.acceptAppointment(
                appointmentId, doctor.getId());

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

        String email = auth.getName();
        log.info("❌ Médecin {} rejette le rendez-vous {}", email, appointmentId);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin introuvable"));

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new RuntimeException("Une raison est requise pour le rejet");
        }

        AppointmentResponse response = appointmentService.rejectAppointment(
                appointmentId,
                doctor.getId(),
                request.getReason(),
                request.getAvailableHours());

        return ResponseEntity.ok(response);
    }

    /**
     * Obtenir la liste des patients du médecin connecté
     */
    @GetMapping("/patients")
    public ResponseEntity<List<PatientInfoResponse>> getMyPatients(Authentication auth) {
        String email = auth.getName();
        log.info("👥 Médecin {} demande la liste de ses patients", email);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin introuvable"));

        List<PatientInfoResponse> patients = appointmentService.getDoctorPatients(doctor.getId());

        return ResponseEntity.ok(patients);
    }

    /**
     * Obtenir les statistiques du tableau de bord
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DoctorStatsResponse> getDashboardStats(Authentication auth) {
        String email = auth.getName();
        log.info("📊 Médecin {} demande les statistiques du tableau de bord", email);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin introuvable"));

        DoctorStatsResponse stats = appointmentService.getDoctorStats(doctor.getId());

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

        log.info("✅ Finalisation du rendez-vous : {}", appointmentId);

        String diagnosis = body.get("diagnosis");
        String prescription = body.get("prescription");
        String notes = body.get("notes");

        AppointmentResponse response = appointmentService.completeAppointment(
                appointmentId, diagnosis, prescription, notes);

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

        log.info("❌ Annulation du rendez-vous : {}", appointmentId);

        String reason = (body != null) ? body.get("reason") : "Aucune raison fournie";

        appointmentService.cancelAppointment(appointmentId, "DOCTOR", reason);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Rendez-vous annulé avec succès"
        ));
    }
}
