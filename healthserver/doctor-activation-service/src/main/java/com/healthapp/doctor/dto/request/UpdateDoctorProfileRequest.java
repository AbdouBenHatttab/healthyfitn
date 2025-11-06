
package com.healthapp.doctor.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDoctorProfileRequest {
    
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String specialization;
    private String hospitalAffiliation;
    private Integer yearsOfExperience;
    private String officeAddress;
    private String consultationHours;
}
