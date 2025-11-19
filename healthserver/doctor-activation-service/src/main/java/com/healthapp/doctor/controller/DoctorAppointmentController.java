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
 * Doctor Appointment Controller
 * Handles all appointment-related operations for doctors
 *
 * NOTE: Public endpoints (for patients) are in PublicDoctorController
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
     * Get all appointments for the authenticated doctor
     */
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(Authentication auth) {
        String email = auth.getName();
        log.info("📅 Doctor {} requesting appointments", email);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        List<AppointmentResponse> appointments = appointmentService.getDoctorAppointments(doctor.getId());

        return ResponseEntity.ok(appointments);
    }

    /**
     * Get upcoming appointments only
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<AppointmentResponse>> getUpcomingAppointments(Authentication auth) {
        String email = auth.getName();
        log.info("📅 Doctor {} requesting upcoming appointments", email);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        List<AppointmentResponse> appointments = appointmentService.getUpcomingAppointments(doctor.getId());

        return ResponseEntity.ok(appointments);
    }

    /**
     * ✅ NEW: Get pending appointments (need response)
     */
    @GetMapping("/pending")
    public ResponseEntity<List<AppointmentResponse>> getPendingAppointments(Authentication auth) {
        String email = auth.getName();
        log.info("📋 Doctor {} requesting pending appointments", email);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        List<AppointmentResponse> appointments = appointmentService.getPendingAppointments(doctor.getId());

        return ResponseEntity.ok(appointments);
    }

    /**
     * ✅ NEW: Accept a pending appointment
     */
    @PostMapping("/{appointmentId}/accept")
    public ResponseEntity<AppointmentResponse> acceptAppointment(
            @PathVariable String appointmentId,
            Authentication auth) {

        String email = auth.getName();
        log.info("✅ Doctor {} accepting appointment {}", email, appointmentId);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        AppointmentResponse response = appointmentService.acceptAppointment(
                appointmentId, doctor.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ NEW: Reject a pending appointment with reason
     */
    @PostMapping("/{appointmentId}/reject")
    public ResponseEntity<AppointmentResponse> rejectAppointment(
            @PathVariable String appointmentId,
            @RequestBody AppointmentResponseRequest request,
            Authentication auth) {

        String email = auth.getName();
        log.info("❌ Doctor {} rejecting appointment {}", email, appointmentId);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new RuntimeException("Reason is required for rejection");
        }

        AppointmentResponse response = appointmentService.rejectAppointment(
                appointmentId,
                doctor.getId(),
                request.getReason(),
                request.getAvailableHours());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all patients for the authenticated doctor
     */
    @GetMapping("/patients")
    public ResponseEntity<List<PatientInfoResponse>> getMyPatients(Authentication auth) {
        String email = auth.getName();
        log.info("👥 Doctor {} requesting patient list", email);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        List<PatientInfoResponse> patients = appointmentService.getDoctorPatients(doctor.getId());

        return ResponseEntity.ok(patients);
    }

    /**
     * Get dashboard statistics
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DoctorStatsResponse> getDashboardStats(Authentication auth) {
        String email = auth.getName();
        log.info("📊 Doctor {} requesting dashboard stats", email);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        DoctorStatsResponse stats = appointmentService.getDoctorStats(doctor.getId());

        return ResponseEntity.ok(stats);
    }

    /**
     * Complete an appointment
     */
    @PostMapping("/{appointmentId}/complete")
    public ResponseEntity<AppointmentResponse> completeAppointment(
            @PathVariable String appointmentId,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        log.info("✅ Completing appointment: {}", appointmentId);

        String diagnosis = body.get("diagnosis");
        String prescription = body.get("prescription");
        String notes = body.get("notes");

        AppointmentResponse response = appointmentService.completeAppointment(
                appointmentId, diagnosis, prescription, notes);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel an appointment (Doctor side)
     */
    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<Map<String, String>> cancelAppointment(
            @PathVariable String appointmentId,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        log.info("❌ Cancelling appointment: {}", appointmentId);

        String reason = (body != null) ? body.get("reason") : "No reason provided";

        appointmentService.cancelAppointment(appointmentId, "DOCTOR", reason);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Appointment cancelled successfully"
        ));
    }
}