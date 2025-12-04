package com.healthapp.doctor.controller;

import com.healthapp.doctor.repository.AppointmentRepository;
import com.healthapp.doctor.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur public pour la gestion des rendez-vous côté patient
 */
@RestController
@RequestMapping("/api/public/doctors/appointments")
@RequiredArgsConstructor
@Slf4j
public class PublicDoctorAppointmentController {

    private final AppointmentService appointmentService;
    @Autowired
    private final AppointmentRepository appointmentRepository;

    /**
     * Annulation d'un rendez-vous par le patient
     *
     * @param appointmentId ID du rendez-vous à annuler
     * @param body Contient la raison de l'annulation (optionnelle)
     */
    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<Map<String, String>> cancelAppointmentByPatient(
            @PathVariable String appointmentId,
            @RequestBody Map<String, String> body) {

        log.info("❌ Patient annule le rendez-vous : {}", appointmentId);

        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            reason = "Aucune raison fournie";
        }
        String doctorId = appointmentRepository.findById(appointmentId).get().getDoctorId();

        // ✅ ICI LA CORRECTION IMPORTANTE
        appointmentService.cancelAppointment(
                appointmentId,
                doctorId,      // doctorId
                body.get("patientId"),     // cancelledBy
                reason
        );

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Rendez-vous annulé avec succès"
        ));
    }
}
