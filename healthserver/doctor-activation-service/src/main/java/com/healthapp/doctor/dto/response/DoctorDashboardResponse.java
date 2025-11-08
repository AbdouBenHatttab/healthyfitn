package com.healthapp.doctor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DoctorDashboardResponse - Dashboard statistics for a doctor
 * 
 * Contains key metrics and statistics about the doctor's practice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDashboardResponse {
    
    // Doctor Information
    private String doctorId;
    private String doctorName;
    private String specialization;
    
    // Main Statistics
    private Integer totalPatients;
    private Integer totalConsultations;
    private Integer upcomingAppointments;
    private Double averageRating;
    
    // Today's Activity
    private Integer todayAppointments;
    private Integer completedAppointmentsToday;
    private Integer pendingAppointments;
    
    // Recent Activity
    private Integer thisWeekAppointments;
    private Integer thisMonthConsultations;
    private Integer newPatientsThisMonth;
    
    // Quick Stats
    private Integer activePatients;
    private Double consultationGrowthRate;
    
    // Metadata
    private LocalDateTime lastLoginAt;
    private LocalDateTime generatedAt;
    private String profilePictureUrl;

}