package com.healthapp.doctor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorStatsResponse {

    private String doctorId;
    private String doctorName;
    private String specialization;

    // Today's Stats
    private Integer todayAppointments;
    private Integer todayCompleted;
    private Integer todayPending;

    // NEW: Pending appointments needing response
    private Integer pendingAppointments;

    // Overall Stats
    private Integer totalAppointments;
    private Integer totalPatients;
    private Integer upcomingAppointments;
    private Integer completedAppointments;
    private Integer cancelledAppointments;

    // This Week/Month
    private Integer thisWeekAppointments;
    private Integer thisMonthAppointments;

    private LocalDateTime generatedAt;
}