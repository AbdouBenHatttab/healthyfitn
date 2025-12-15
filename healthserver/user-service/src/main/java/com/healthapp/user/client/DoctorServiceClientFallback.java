package com.healthapp.user.client;

import com.healthapp.user.dto.response.CancelAppointmentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fallback pour DoctorServiceClient
 * Fournit des réponses par défaut quand le Doctor Service est indisponible
 */
@Component
@Slf4j
public class DoctorServiceClientFallback implements DoctorServiceClient {

    @Override
    public Map<String, Object> createAppointmentFromPatient(Map<String, Object> request) {
        log.error("🔴 Doctor Service unavailable - Cannot create appointment");
        return Map.of(
                "error", "Service Unavailable",
                "message", "Le service de rendez-vous est temporairement indisponible. Veuillez réessayer plus tard."
        );
    }

    @Override
    public List<Map<String, Object>> getPatientAppointments(String patientId) {
        log.error("🔴 Doctor Service unavailable - Cannot fetch appointments for patient: {}", patientId);
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> cancelAppointment(String appointmentId, CancelAppointmentRequest request) {
        log.error("🔴 Doctor Service unavailable - Cannot cancel appointment: {}", appointmentId);
        return Map.of(
                "error", "Service Unavailable",
                "message", "Impossible d'annuler le rendez-vous pour le moment. Veuillez réessayer plus tard."
        );
    }

    @Override
    public List<Map<String, Object>> getActivatedDoctors() {
        log.error("🔴 Doctor Service unavailable - Cannot fetch activated doctors");
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> updateAppointmentsPatientEmail(String oldEmail, Map<String, String> body) {
        log.error("🔴 Doctor Service unavailable - Cannot update patient email");
        return Map.of(
                "error", "Service Unavailable",
                "message", "Impossible de mettre à jour l'email pour le moment."
        );
    }

    @Override
    public Map<String, Object> deletePatientAppointments(String patientId, String patientEmail) {
        log.error("🔴 Doctor Service unavailable - Cannot delete patient appointments");
        return Map.of(
                "error", "Service Unavailable",
                "message", "Impossible de supprimer les rendez-vous pour le moment."
        );
    }
}