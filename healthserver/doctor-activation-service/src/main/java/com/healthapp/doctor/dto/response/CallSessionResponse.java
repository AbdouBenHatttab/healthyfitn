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
public class CallSessionResponse {
    
    private String callId;
    private String appointmentId;
    
    private String doctorId;
    private String doctorEmail;
    private String patientId;
    private String patientEmail;
    
    private String callType; // AUDIO, VIDEO
    private String status;   // INITIATED, RINGING, ACTIVE, ENDED
    private String initiatorRole; // DOCTOR, PATIENT
    
    private String iceServers; // JSON
    private String offerSdp;
    private String answerSdp;
    
    private LocalDateTime createdAt;
}