package com.healthapp.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelAppointmentRequest {
    private String reason;
    private String patientId;
}
