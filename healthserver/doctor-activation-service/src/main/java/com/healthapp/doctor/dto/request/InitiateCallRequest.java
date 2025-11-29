package com.healthapp.doctor.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateCallRequest {
    
    @NotBlank(message = "Appointment ID is required")
    private String appointmentId;
    
    @NotBlank(message = "Call type is required")
    private String callType; // AUDIO or VIDEO
}
