package com.healthapp.user.controller;

import com.healthapp.user.dto.request.AppointmentRequest;
import com.healthapp.user.dto.response.ApiResponse;
import com.healthapp.user.dto.response.AppointmentResponse;
import com.healthapp.user.security.SecurityHelper;
import com.healthapp.user.security.CustomUserPrincipal;
import com.healthapp.user.service.AppointmentClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Slf4j
public class PatientAppointmentController {

    private final AppointmentClientService appointmentService;
    private final SecurityHelper securityHelper;

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody AppointmentRequest request,
            Authentication auth) {

        CustomUserPrincipal principal = securityHelper.getPrincipal(auth);

        log.info("📅 Patient {} crée un rendez-vous avec le médecin {}",
                principal.getEmail(), request.getDoctorId());

        AppointmentResponse response = appointmentService.createAppointment(request, principal);

        return ResponseEntity.ok(ApiResponse.success("Rendez-vous créé avec succès", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getMyAppointments(Authentication auth) {
        String userId = securityHelper.getUserId(auth);

        log.info("📅 Patient {} demande ses rendez-vous", securityHelper.getUserEmail(auth));

        List<AppointmentResponse> appointments = appointmentService.getPatientAppointments(userId);

        return ResponseEntity.ok(ApiResponse.success("Rendez-vous récupérés avec succès", appointments));
    }

    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelAppointment(
            @PathVariable String appointmentId,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        String userEmail = securityHelper.getUserEmail(auth);

        log.info("❌ Patient {} annule le rendez-vous : {}", userEmail, appointmentId);

        String reason = body.get("reason");
        CustomUserPrincipal principal = securityHelper.getPrincipal(auth);
        String patientId = principal.getId(); // MongoDB ID

        appointmentService.cancelAppointment(appointmentId, reason, patientId);

        return ResponseEntity.ok(ApiResponse.success("Rendez-vous annulé avec succès", null));
    }

    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableDoctors() {
        log.info("🩺 Récupération des médecins disponibles");

        List<Map<String, Object>> doctors = appointmentService.getAvailableDoctors();

        return ResponseEntity.ok(ApiResponse.success("Médecins récupérés avec succès", doctors));
    }
}