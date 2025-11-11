package com.healthapp.user.service;

import com.healthapp.user.client.DoctorServiceClient;
import com.healthapp.user.dto.request.AppointmentRequest;
import com.healthapp.user.dto.response.AppointmentResponse;
import com.healthapp.user.entity.User;
import com.healthapp.user.repository.UserRepository;
import com.healthapp.user.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
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
        appointmentData.put("patientPhone", user.getPhoneNumber());
        appointmentData.put("appointmentDateTime", request.getAppointmentDateTime().toString());
        appointmentData.put("appointmentType", request.getAppointmentType());
        appointmentData.put("reason", request.getReason());
        appointmentData.put("notes", request.getNotes());

        try {
            Map<String, Object> response = doctorServiceClient.createAppointmentFromPatient(appointmentData);
            return mapToAppointmentResponse(response);
        } catch (Exception e) {
            log.error("❌ Failed to create appointment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create appointment: " + e.getMessage(), e);
        }
    }

    /**
     * Get all appointments for a patient (from doctor-service)
     */
    public List<AppointmentResponse> getPatientAppointments(String patientId) {
        try {
            List<Map<String, Object>> appointments = doctorServiceClient.getPatientAppointments(patientId);
            return appointments.stream()
                    .map(this::mapToAppointmentResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ Failed to fetch appointments: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch appointments: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel an existing appointment
     */
    public void cancelAppointment(String appointmentId, String cancelledBy, String reason) {
        log.info("========================================");
        log.info("🚫 CANCEL APPOINTMENT SERVICE CALLED");
        log.info("========================================");
        log.info("📋 Appointment ID: {}", appointmentId);
        log.info("👤 Cancelled By: {}", cancelledBy);
        log.info("📝 Reason: {}", reason);
        log.info("========================================");

        Map<String, String> body = new HashMap<>();
        body.put("cancelledBy", cancelledBy);
        body.put("reason", reason);

        try {
            log.info("📡 Calling DoctorServiceClient.cancelAppointment()...");
            log.info("🎯 Target: http://localhost:8083/api/doctors/appointments/{}/cancel", appointmentId);
            log.info("📦 Body: {}", body);

            // Get current request to check auth header
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                log.info("🔐 Authorization header in current request: {}",
                        authHeader != null ? "Present (length: " + authHeader.length() + ")" : "NOT PRESENT");
            } else {
                log.warn("⚠️ No request attributes available");
            }

            doctorServiceClient.cancelAppointment(appointmentId, body);

            log.info("========================================");
            log.info("✅ APPOINTMENT CANCELLED SUCCESSFULLY");
            log.info("========================================");

        } catch (feign.FeignException.FeignClientException e) {
            log.error("========================================");
            log.error("❌ FEIGN CLIENT EXCEPTION");
            log.error("========================================");
            log.error("📊 Status Code: {}", e.status());
            log.error("📨 Response Body: {}", e.contentUTF8());
            log.error("🔗 Request URL: {}", e.request() != null ? e.request().url() : "unknown");
            log.error("📋 Request Body: {}", e.request() != null ? new String(e.request().body()) : "unknown");
            log.error("========================================");
            throw new RuntimeException("Failed to cancel appointment - Status " + e.status() + ": " + e.contentUTF8(), e);

        } catch (feign.FeignException e) {
            log.error("========================================");
            log.error("❌ FEIGN EXCEPTION (General)");
            log.error("========================================");
            log.error("📊 Status Code: {}", e.status());
            log.error("📄 Message: {}", e.getMessage());
            log.error("📨 Content: {}", e.contentUTF8());
            log.error("========================================");
            throw new RuntimeException("Failed to cancel appointment: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("========================================");
            log.error("❌ UNEXPECTED EXCEPTION");
            log.error("========================================");
            log.error("🔥 Exception Type: {}", e.getClass().getName());
            log.error("📄 Message: {}", e.getMessage());
            log.error("📚 Stack Trace:", e);
            log.error("========================================");
            throw new RuntimeException("Failed to cancel appointment: " + e.getMessage(), e);
        }
    }

    /**
     * Get available doctors (from doctor-service)
     */
    public List<Map<String, Object>> getAvailableDoctors() {
        try {
            return doctorServiceClient.getActivatedDoctors();
        } catch (Exception e) {
            log.error("❌ Failed to fetch available doctors: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch available doctors: " + e.getMessage(), e);
        }
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
                .patientPhone((String) data.get("patientPhone"))
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