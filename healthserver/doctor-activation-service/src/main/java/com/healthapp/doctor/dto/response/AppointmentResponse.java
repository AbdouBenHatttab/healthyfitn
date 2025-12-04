package com.healthapp.doctor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private String id;

    // Patient Info
    private String patientId;
    private String patientEmail;
    private String patientName;
    private String patientPhone;

    // Doctor Info
    private String doctorId;
    private String doctorEmail;
    private String doctorName;
    private String specialization;

    // Appointment Details
    private LocalDateTime appointmentDateTime;
    private String appointmentType;
    private String reason;
    private String notes;
    private String status;

    // ✅ RÉPONSE DU DOCTEUR
    private String doctorResponse;
    private String doctorResponseReason;
    private String availableHoursSuggestion;
    private LocalDateTime respondedAt;

    // ✅ CONSULTATION
    private String diagnosis;
    private String prescription;
    private String doctorNotes;
    private LocalDateTime completedAt;

    // ✅ ANNULATION
    private String cancelledBy;
    private String cancellationReason;
    private LocalDateTime cancelledAt;

    // Audit
    private LocalDateTime createdAt;
}
