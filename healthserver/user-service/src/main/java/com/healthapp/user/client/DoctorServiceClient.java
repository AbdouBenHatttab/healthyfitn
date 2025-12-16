package com.healthapp.user.client;

import com.healthapp.user.dto.response.CancelAppointmentRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for Doctor Service
 * Communicates with doctor-activation-service endpoints
 *
 * ✅ CORRECTION: Ajout de fallbackFactory pour une meilleure gestion d'erreurs
 */
@FeignClient(
        name = "doctor-activation-service",
        url = "http://localhost:8083",
        configuration = com.healthapp.user.config.FeignClientConfig.class,
        fallbackFactory = DoctorServiceClientFallback.class  // ✅ Changé de fallback à fallbackFactory
)
public interface DoctorServiceClient {

    @PostMapping("/api/doctors/appointments/from-patient")
    Map<String, Object> createAppointmentFromPatient(@RequestBody Map<String, Object> request);

    @GetMapping("/api/doctors/appointments/patient/{patientId}")
    List<Map<String, Object>> getPatientAppointments(@PathVariable String patientId);

    /**
     * Cancel an appointment for a patient
     */
    @PostMapping("/api/public/doctors/appointments/{appointmentId}/cancel")
    Map<String, String> cancelAppointment(
            @PathVariable("appointmentId") String appointmentId,
            @RequestBody CancelAppointmentRequest request
    );

    @GetMapping("/api/doctors/available")
    List<Map<String, Object>> getActivatedDoctors();

    @PutMapping("/api/public/doctors/appointments/{oldEmail}")
    Map<String, String> updateAppointmentsPatientEmail(
            @PathVariable("oldEmail") String oldEmail,
            @RequestBody Map<String, String> body
    );

    @DeleteMapping("/api/doctors/appointments/patient/{patientId}")
    Map<String, Object> deletePatientAppointments(
            @PathVariable("patientId") String patientId,
            @RequestParam(value = "patientEmail", required = false) String patientEmail
    );
}