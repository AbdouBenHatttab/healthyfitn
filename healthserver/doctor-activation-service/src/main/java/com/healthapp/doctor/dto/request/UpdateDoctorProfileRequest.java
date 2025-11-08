
package com.healthapp.doctor.dto.request;

import jakarta.validation.constraints.Email;
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
   // ✅ NOUVEAU: Permettre de modifier le contact email
    @Email(message = "Invalid email format")
    private String contactEmail;
    private String specialization;
    private String hospitalAffiliation;
    private Integer yearsOfExperience;
    private String officeAddress;
    private String consultationHours;
    private String profilePictureUrl;
}
