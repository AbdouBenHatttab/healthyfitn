package com.healthapp.doctor.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Appointment Entity - Manages appointments between patients and doctors
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "appointments")
public class Appointment {

    @Id
    private String id;

    // Patient Information
    @Indexed
    private String patientId;
    private String patientEmail;
    private String patientName;
    private String patientPhone;

    // Doctor Information
    @Indexed
    private String doctorId;
    private String doctorEmail;
    private String doctorName;
    private String specialization;

    // Appointment Details
    @Indexed
    private LocalDateTime appointmentDateTime;
    private String appointmentType;   // CONSULTATION, FOLLOW_UP, EMERGENCY
    private String reason;
    private String notes;

    // ✅ Doctor Response Fields (consolidated - removed duplicates)
    @Field("doctor_response")
    private String doctorResponse; // "ACCEPTED" or "REJECTED"

    @Field("doctor_response_reason")
    private String doctorResponseReason; // Why rejected

    @Field("available_hours_suggestion")
    private String availableHoursSuggestion; // e.g., "8:00 AM - 4:00 PM"

    @Field("responded_at")
    private LocalDateTime respondedAt; // When doctor responded

    // Status Management
    @Indexed
    @Builder.Default
    private String status = "PENDING"; // PENDING, SCHEDULED, CONFIRMED, COMPLETED, CANCELLED, REJECTED, NO_SHOW

    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private String cancelledBy;       // PATIENT or DOCTOR

    // Consultation Details (filled after appointment)
    private String diagnosis;
    private String prescription;
    private String doctorNotes;
    private LocalDateTime completedAt;

    // Metadata
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Business Methods
    public boolean isUpcoming() {
        return "SCHEDULED".equals(status) && appointmentDateTime.isAfter(LocalDateTime.now());
    }

    public boolean isPast() {
        return appointmentDateTime.isBefore(LocalDateTime.now());
    }

    public boolean canBeCancelled() {
        return ("SCHEDULED".equals(status) || "PENDING".equals(status))
                && appointmentDateTime.isAfter(LocalDateTime.now());
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }
}