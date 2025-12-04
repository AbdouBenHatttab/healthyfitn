package com.healthapp.user.service;

import com.healthapp.user.client.DoctorServiceClient;
import com.healthapp.user.dto.request.AppointmentRequest;
import com.healthapp.user.dto.response.AppointmentResponse;
import com.healthapp.user.dto.response.CancelAppointmentRequest;
import com.healthapp.user.entity.User;
import com.healthapp.user.repository.UserRepository;
import com.healthapp.user.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentClientService {

    private final DoctorServiceClient doctorServiceClient;
    private final UserRepository userRepository;

    /**
     * Create appointment for a patient (calls doctor-service)
     */
    public AppointmentResponse createAppointment(AppointmentRequest request, CustomUserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("doctorId", request.getDoctorId());
        appointmentData.put("patientId", user.getId());
        appointmentData.put("patientEmail", user.getEmail());
        appointmentData.put("patientName", user.getFullName());
        appointmentData.put("patientPhone", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");


        // ✅ CRITICAL FIX: Use ISO-8601 format with milliseconds
        appointmentData.put("appointmentDateTime",
                request.getAppointmentDateTime().toString());

        appointmentData.put("appointmentType", request.getAppointmentType());
        appointmentData.put("reason", request.getReason());
        appointmentData.put("notes", request.getNotes());

        log.info("📤 Sending appointment data to doctor-service: {}", appointmentData);

        try {
            Map<String, Object> response = doctorServiceClient.createAppointmentFromPatient(appointmentData);
            log.info("✅ Appointment created successfully: {}", response);
            return mapToAppointmentResponse(response);
        } catch (Exception e) {
            log.error("❌ Failed to create appointment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create appointment: " + e.getMessage());
        }
    }
    /**
     * Get all appointments for a patient (from doctor-service)
     */
    public List<AppointmentResponse> getPatientAppointments(String patientId) {
        List<Map<String, Object>> appointments = doctorServiceClient.getPatientAppointments(patientId);
        return appointments.stream()
                .map(this::mapToAppointmentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cancel an existing appointment
     */
    public void cancelAppointment(String appointmentId, String reason,String patientId) {
        if (reason == null || reason.isBlank()) {
            reason = "No reason provided";
        }

        CancelAppointmentRequest request = new CancelAppointmentRequest(reason,patientId);

        log.info("Calling doctor-service cancel endpoint for appointmentId={}", appointmentId);
        Map<String, String> response = doctorServiceClient.cancelAppointment(appointmentId, request);
        log.info("Doctor-service response: {}", response);
    }

    /**
     * Get available doctors (from doctor-service)
     */
    public List<Map<String, Object>> getAvailableDoctors() {
        return doctorServiceClient.getActivatedDoctors();
    }

    /**
     * Convert raw map data to AppointmentResponse DTO
     */
    private AppointmentResponse mapToAppointmentResponse(Map<String, Object> data) {
        return AppointmentResponse.builder()
                .id((String) data.get("id"))
                .patientId((String) data.get("patientId"))
                .patientEmail((String) data.get("patientEmail"))
                .patientName((String) data.get("patientName"))
                .patientPhone(data.get("patientPhone") != null ? (String) data.get("patientPhone") : "")
                .doctorId((String) data.get("doctorId"))
                .doctorEmail((String) data.get("doctorEmail"))
                .doctorName((String) data.get("doctorName"))
                .specialization((String) data.get("specialization"))
                .appointmentDateTime(data.get("appointmentDateTime") != null ?
                        java.time.LocalDateTime.parse((String) data.get("appointmentDateTime")) : null)
                .appointmentType((String) data.get("appointmentType"))
                .reason((String) data.get("reason"))
                .notes((String) data.get("notes"))
                .status((String) data.get("status"))
                .build();
    }
}
