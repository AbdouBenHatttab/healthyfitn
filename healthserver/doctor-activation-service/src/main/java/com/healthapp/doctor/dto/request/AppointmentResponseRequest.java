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
public class AppointmentResponseRequest {

    @NotBlank(message = "Action is required (ACCEPT or REJECT)")
    private String action; // "ACCEPT" or "REJECT"

    private String reason; // Required only for REJECT

    private String availableHours; // e.g., "8:00 AM - 4:00 PM"
}